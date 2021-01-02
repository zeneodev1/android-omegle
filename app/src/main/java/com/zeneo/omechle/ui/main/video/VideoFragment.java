package com.zeneo.omechle.ui.main.video;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.zeneo.omechle.JSCallback;
import com.zeneo.omechle.R;
import com.zeneo.omechle.adapter.MessagesListAdapter;
import com.zeneo.omechle.constant.State;
import com.zeneo.omechle.databinding.FragmentVideoBinding;
import com.zeneo.omechle.model.Message;
import com.zeneo.omechle.model.Room;
import com.zeneo.omechle.network.client.MyWebViewClient;
import com.zeneo.omechle.network.stomp.Stomp;
import com.zeneo.omechle.repository.MatchingRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoFragment extends Fragment implements JSCallback {

    private final static String TAG = "fuck";
    private final static int REQUEST_CODE = 158;

    private String userId;
    private ObservableField<State> state = new ObservableField<>();
    private Room currentRoom;
    private List<Message> messages = new ArrayList<>();
    private MatchingRepository matchingRepository;
    private MessagesListAdapter adapter;
    private ObservableInt count = new ObservableInt(0);
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private boolean isSwipeInstructionShown;
    private InterstitialAd interstitialAd;
    private InterstitialAdListener interstitialAdListener;

    private EditText messageEditText;
    private WebView webView;
    private LinearLayout swipeGuideLinearLayout, inputChatContainerLinearLayout;
    private ImageButton sendButton;
    private RecyclerView messagesRecyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission(REQUEST_CODE);
        isSwipeInstructionShown = false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        interstitialAd = new InterstitialAd(context, "YOUR_PLACEMENT_ID");
        interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                // Interstitial ad displayed callback
                Log.e(TAG, "Interstitial ad displayed.");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                // Interstitial dismissed callback
                Log.e(TAG, "Interstitial ad dismissed.");
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Ad error callback
                Log.e(TAG, "Interstitial ad failed to load: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Interstitial ad is loaded and ready to be displayed
                Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!");
                // Show the ad
                //interstitialAd.show();
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
                Log.d(TAG, "Interstitial ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
                Log.d(TAG, "Interstitial ad impression logged!");
            }
        };
        interstitialAd.loadAd(
                interstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());
    }

    // Function to check and request permission
    public void checkPermission(int requestCode) {

        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.MODIFY_AUDIO_SETTINGS},
                            requestCode);
        } else {
            Log.d(TAG, "Permissions already granted!");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), "Permissions Granted!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, String.valueOf(grantResults.length));
        } else {
            Log.d(TAG, "Permissions Denied!");
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentVideoBinding videoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video, container, false);
        videoBinding.setState(state);
        videoBinding.setCount(count);
        return videoBinding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(interstitialAd.isAdLoaded()){
            interstitialAd.show();
        }

        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initViews(view);

        disableChat();

        sendButton.setOnClickListener(v -> {
            sendMessage();
        });

        swipeGuideLinearLayout.setVisibility(View.GONE);

        adapter = new MessagesListAdapter(messages, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        messagesRecyclerView.setLayoutManager(linearLayoutManager);
        messagesRecyclerView.setAdapter(adapter);

        state.set(State.CONNECTING);
        matchingRepository = new MatchingRepository();
        matchingRepository.connect((status, frame) -> {
            if (status == Stomp.CONNECTED) {
                userId = frame.getHeaders().get("user-name");
                setupWebView();
            }
        });

        webView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            public void onSwipeLeft() {
                leftRoom();
                next();
                disableChat();
            }
        });

    }

    private void initViews(View view) {
        messageEditText = view.findViewById(R.id.message_input_video);
        webView = view.findViewById(R.id.call_web_view);
        inputChatContainerLinearLayout = view.findViewById(R.id.input_chat_container_ll);
        sendButton = view.findViewById(R.id.send_button_video);
        swipeGuideLinearLayout = view.findViewById(R.id.swipe_left_ll);
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
    }

    private void disableChat() {
        inputChatContainerLinearLayout.setEnabled(false);
        sendButton.setEnabled(false);
        messageEditText.setEnabled(false);
    }

    private void enableChat() {
        inputChatContainerLinearLayout.setEnabled(true);
        messageEditText.setEnabled(true);
        sendButton.setEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.post(() -> {
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    request.grant(request.getResources());
                }

                @Override
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    Log.d("MyApplication", cm.message() + " -- From line "
                            + cm.lineNumber() + " of "
                            + cm.sourceId());
                    return true;
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            webView.addJavascriptInterface(this, "Android");
            loadWebView();
        });
    }

    private void loadWebView() {
        webView.loadUrl("file:android_asset/call.html");
        webView.setWebViewClient(new MyWebViewClient(this::initializePeer));
    }

    private void initializePeer() {
        callJavascriptFunction("javascript:init(\"" + userId + "\")");
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

            if (currentRoom.getUsers().get(0).getId().equals(userId)) {
                callJavascriptFunction("javascript:startCall(\"" + currentRoom.getUsers().get(1).getId() + "\")");
            }
            watchRoom();

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                enableChat();
                if (!isSwipeInstructionShown) {
                    animateSwipeGuide();
                    isSwipeInstructionShown = true;
                }
            });
        });
    }

    private void animateSwipeGuide() {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        swipeGuideLinearLayout.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(1000);
        animation.setStartOffset(5000);
        animation.setFillAfter(true);
        swipeGuideLinearLayout.startAnimation(animation);
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
                    messages.add(0, message);
                    //messages.add(message);
                    count.set(count.get() + 1);
                    Log.d(TAG, "Current data: " + messages.toString());
                    adapter.notifyDataSetChanged();
                    messagesRecyclerView.scrollToPosition(0);
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
            matchingRepository.watchRoom(currentRoom.getId(), (headers, body) -> {
                Map data = new Gson().fromJson(body, Map.class);
                String type = String.valueOf(data.get("type"));
                if (type.equals("EXIT")) {
                    leftRoom();
                    next();
                    inputChatContainerLinearLayout.setEnabled(false);
                    messageEditText.setEnabled(false);
                    sendButton.setEnabled(false);
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
            callJavascriptFunction("javascript:stopCall()");
            state.set(State.LEFT);
            messages.clear();
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
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
        super.onDestroy();
        matchingRepository.disconnect();
        webView.destroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


}