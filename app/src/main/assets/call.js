let localVideo = document.getElementById("local-video")
let remoteVideo = document.getElementById("remote-video")

localVideo.style.opacity = 0
remoteVideo.style.opacity = 0

localVideo.onplaying = () => { localVideo.style.opacity = 1 }
remoteVideo.onplaying = () => { remoteVideo.style.opacity = 1 }

let currentCall;

let peer
function init(userId) {
    peer = new Peer(userId)

    peer.on('open', () => {

    navigator.getUserMedia({
                    audio: true,
                    video: true
                }, (stream) => {
                    localVideo.srcObject = stream
                    Android.onPeerConnected();

                })

        listen()
    })


}

let localStream
function listen() {
    peer.on('call', (call) => {

        navigator.getUserMedia({
            audio: true,
            video: true
        }, (stream) => {

            currentCall = call;

            localStream = stream

            call.answer(stream)
            call.on('stream', (remoteStream) => {
                remoteVideo.srcObject = remoteStream
            })

        })

    })
}

function startCall(otherUserId) {
    navigator.getUserMedia({
        audio: true,
        video: true
    }, (stream) => {

        localStream = stream

        const call = peer.call(otherUserId, stream)
        currentCall = call;
        call.on('stream', (remoteStream) => {
            console.log(remoteStream.active);
            remoteVideo.srcObject = remoteStream
        })

    })
}


function stopCall() {
   remoteVideo.srcObject = null;
   if (currentCall !== null) {
       currentCall.close();
       currentCall = null;
   }

}

function toggleVideo(b) {
    if (b == "true") {
        localStream.getVideoTracks()[0].enabled = true
    } else {
        localStream.getVideoTracks()[0].enabled = false
    }
}

function toggleAudio(b) {
    if (b == "true") {
        localStream.getAudioTracks()[0].enabled = true
    } else {
        localStream.getAudioTracks()[0].enabled = false
    }
}