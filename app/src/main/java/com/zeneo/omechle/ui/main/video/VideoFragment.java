package com.zeneo.omechle.ui.main.video;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.zeneo.omechle.JSCallback;
import com.zeneo.omechle.R;
import com.zeneo.omechle.adapter.MessagesListAdapter;
import com.zeneo.omechle.constant.State;
import com.zeneo.omechle.databinding.FragmentVideoBinding;
import com.zeneo.omechle.model.Message;
import com.zeneo.omechle.model.Room;
import com.zeneo.omechle.network.client.MyWebChromeClient;
import com.zeneo.omechle.network.client.MyWebViewClient;
import com.zeneo.omechle.network.stomp.Stomp;
import com.zeneo.omechle.repository.MatchingRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VideoFragment extends Fragment implements JSCallback {



    private final static String TAG = "Video Fragment";
    private String userId;
    private ObservableField<State> state = new ObservableField<>();
    private Room currentRoom;
    private List<Message> messages = new ArrayList<>();

    private MatchingRepository matchingRepository;

    private EditText messageEditText;

    private WebView webView;

    private MessagesListAdapter adapter;

    private ObservableInt count = new ObservableInt(0);

    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentVideoBinding videoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video, container, false);
        videoBinding.setState(state);
        videoBinding.setCount(count);
        return videoBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        RecyclerView messagesList = view.findViewById(R.id.messages_list);
        adapter = new MessagesListAdapter(messages, getContext());
        messagesList.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesList.setAdapter(adapter);

        state.set(State.CONNECTING);
        matchingRepository = new MatchingRepository();
        matchingRepository.connect((status, frame) -> {
            if (status == Stomp.CONNECTED) {
                userId = frame.getHeaders().get("user-name");
                setupWebView();
            }
        });

    }

    void initViews(View view) {
        // init views
        messageEditText = view.findViewById(R.id.message_input);
        ImageButton sendButton = view.findViewById(R.id.send_button);
        ImageButton stopButton = view.findViewById(R.id.stop_button);
        ImageButton nextButton = view.findViewById(R.id.next_button);
        webView = view.findViewById(R.id.call_web_view);
        sendButton.setOnClickListener(v -> {
            sendMessage();
        });
        stopButton.setOnClickListener(v -> {
            leftRoom();
        });
        nextButton.setOnClickListener(v -> {
            next();
        });
    }

    private void setupWebView() {
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        webView.addJavascriptInterface(this, "Android");
        loadWebView();
    }

    private void loadWebView() {
        webView.loadUrl("file:android_asset/call.html");
        webView.setWebViewClient(new MyWebViewClient(this::initializePeer));
    }

    private void initializePeer() {
        callJavascriptFunction("javascript:init(\""+ userId +"\")");
    }

    private void callJavascriptFunction(String f) {
        webView.post(() -> webView.evaluateJavascript(f, null));
    }

    @SuppressLint("CheckResult")
    private void start() {
        state.set(State.IN_QUEUE);
        matchingRepository.startMatching("video");
        matchingRepository.watchMatching(userId, (headers, body) -> {
            matchingRepository.acceptQueue(userId, body);
        });
        matchingRepository.watchAcceptedMatching(userId, (header, body) -> {
            state.set(State.IN_ROOM);
            currentRoom = new Gson().fromJson(body, Room.class);
            watchRoom();
        });
    }

    private void watchRoom() {
        if (state.get() == State.IN_ROOM) {
            firebaseFirestore.collection("rooms").document(currentRoom.getId()).addSnapshotListener((value, error) -> {
                if (value != null && value.exists()) {
                    Log.d(TAG, "Current data: " + value.getData());
                    Message message = Message.fromMap((Map<String, Object>) value.getData().get("message"));
                    if (message.getFrom().equals(userId))
                        message.setMe(true);
                    else
                        message.setMe(false);
                    messages.add(message);
                    count.set(count.get() + 1);
                    Log.d(TAG, "Current data: " + messages.toString());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
            matchingRepository.watchRoom(currentRoom.getId(), (headers, body) -> {
                Map<String, Objects> data = new Gson().fromJson(body, Map.class);
                if (data.get("type").equals("exit")) {
                    leftRoom();
                }
            });
        }
    }

    private void sendMessage() {
        if (state.get() == State.IN_ROOM) {
            Message message = new Message();
            message.setFrom(userId);
            message.setSentAt(new Date(System.currentTimeMillis()));
            message.setRoomId(currentRoom.getId());
            message.setText(messageEditText.getText().toString().trim());
            if (!message.getText().equals("")) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", "message");
                map.put("message", message);
                firebaseFirestore.collection("rooms").document(currentRoom.getId()).set(map);
                messageEditText.setText("");
            }
        }
    }

    public void leftRoom() {
        if (state.get() == State.IN_ROOM) {
            matchingRepository.sendExit(currentRoom.getId());
            state.set(State.LEFT);
        }
    }

    public void next() {
        if (state.get() == State.LEFT) {
            start();
        }
    }

    @Override
    @JavascriptInterface
    public void onPeerConnected() {
        start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        matchingRepository.disconnect();
    }

}