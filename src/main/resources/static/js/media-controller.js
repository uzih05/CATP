/**
 * Media Controller
 * 배경 영상과 BGM 컨트롤 (재생바 + 볼륨 조절 기능)
 */

class MediaController {
    constructor() {
        this.bgVideo = null;
        this.bgAudio = null;
        this.videoBtn = null;
        this.audioBtn = null;

        // 재생바 관련
        this.progressBar = null;
        this.progressFill = null;
        this.timeDisplay = null;

        // 볼륨 관련
        this.volumeSlider = null;

        this.isVideoPlaying = true;
        this.isAudioPlaying = false;
        this.audioVolume = 0.5; // 기본 볼륨 50%
    }

    init() {
        this.createBackgroundVideo();
        this.createBackgroundAudio();
        this.createControls();
        this.bindEvents();
    }

    createBackgroundVideo() {
        const container = document.createElement('div');
        container.className = 'video-background';
        container.id = 'videoBackground';

        this.bgVideo = document.createElement('video');
        this.bgVideo.id = 'bgVideo';
        this.bgVideo.autoplay = true;
        this.bgVideo.muted = true;
        this.bgVideo.loop = true;
        this.bgVideo.playsInline = true;
        this.bgVideo.setAttribute('playsinline', '');

        this.bgVideo.innerHTML = `
            <source src="/assets/videos/background.mp4" type="video/mp4">
            <source src="/assets/videos/background.webm" type="video/webm">
        `;

        const overlay = document.createElement('div');
        overlay.className = 'overlay';

        container.appendChild(this.bgVideo);
        container.appendChild(overlay);
        document.body.insertBefore(container, document.body.firstChild);

        this.bgVideo.addEventListener('error', () => {
            container.classList.add('hidden');
            this.createGradientFallback();
        });

        this.bgVideo.play().catch(() => {});
    }

    createGradientFallback() {
        const gradientBg = document.createElement('div');
        gradientBg.className = 'gradient-background';
        gradientBg.id = 'gradientBackground';
        document.body.insertBefore(gradientBg, document.body.firstChild);

        const orbs = document.createElement('div');
        orbs.className = 'floating-orbs';
        orbs.innerHTML = `
            <div class="orb orb-1"></div>
            <div class="orb orb-2"></div>
            <div class="orb orb-3"></div>
        `;
        document.body.insertBefore(orbs, document.body.children[1]);
    }

    createBackgroundAudio() {
        this.bgAudio = document.createElement('audio');
        this.bgAudio.id = 'bgAudio';
        this.bgAudio.loop = true;
        this.bgAudio.volume = this.audioVolume;

        this.bgAudio.innerHTML = `
            <source src="/assets/audio/bgm.mp3" type="audio/mpeg">
            <source src="/assets/audio/bgm.ogg" type="audio/ogg">
        `;

        document.body.appendChild(this.bgAudio);
    }

    createControls() {
        const controls = document.createElement('div');
        controls.className = 'media-controls';
        controls.id = 'mediaControls';

        // 오디오 플레이어 (재생바 + 시간 + 볼륨)
        const audioPlayer = document.createElement('div');
        audioPlayer.className = 'audio-player';
        audioPlayer.id = 'audioPlayer';

        // [수정] 볼륨 슬라이더 추가
        audioPlayer.innerHTML = `
            <div class="audio-progress-bar" id="audioProgressBar">
                <div class="audio-progress-fill" id="audioProgressFill"></div>
            </div>
            <span class="audio-time" id="audioTime">00:00</span>
            <div class="volume-wrapper">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="volume-icon">
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                    <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                </svg>
                <input type="range" class="volume-slider" id="volumeSlider" min="0" max="1" step="0.1" value="${this.audioVolume}">
            </div>
        `;

        controls.appendChild(audioPlayer);

        this.videoBtn = document.createElement('button');
        this.videoBtn.className = 'media-btn active';
        this.videoBtn.id = 'videoToggle';
        this.videoBtn.innerHTML = this.getVideoIcon(true);

        this.audioBtn = document.createElement('button');
        this.audioBtn.className = 'media-btn';
        this.audioBtn.id = 'audioToggle';
        this.audioBtn.innerHTML = this.getAudioIcon(false);

        controls.appendChild(this.videoBtn);
        controls.appendChild(this.audioBtn);
        document.body.appendChild(controls);

        // 요소 캐싱
        this.progressBar = audioPlayer.querySelector('#audioProgressBar');
        this.progressFill = audioPlayer.querySelector('#audioProgressFill');
        this.timeDisplay = audioPlayer.querySelector('#audioTime');
        this.volumeSlider = audioPlayer.querySelector('#volumeSlider');
    }

    bindEvents() {
        this.videoBtn.addEventListener('click', () => this.toggleVideo());
        this.audioBtn.addEventListener('click', () => this.toggleAudio());

        // 오디오 이벤트
        this.bgAudio.addEventListener('timeupdate', () => this.updateProgressBar());
        this.progressBar.addEventListener('click', (e) => this.seekAudio(e));

        // [추가] 볼륨 변경 이벤트
        this.volumeSlider.addEventListener('input', (e) => {
            this.audioVolume = e.target.value;
            this.bgAudio.volume = this.audioVolume;
        });

        document.addEventListener('visibilitychange', () => {
            if (document.hidden && this.isAudioPlaying) {
                this.bgAudio.pause();
            } else if (!document.hidden && this.isAudioPlaying) {
                this.bgAudio.play().catch(() => {});
            }
        });
    }

    updateProgressBar() {
        if (!this.bgAudio.duration) return;
        const percent = (this.bgAudio.currentTime / this.bgAudio.duration) * 100;
        this.progressFill.style.width = `${percent}%`;
        this.timeDisplay.textContent = this.formatTime(this.bgAudio.currentTime);
    }

    seekAudio(e) {
        const width = this.progressBar.clientWidth;
        const clickX = e.offsetX;
        const duration = this.bgAudio.duration;
        this.bgAudio.currentTime = (clickX / width) * duration;
    }

    formatTime(seconds) {
        const min = Math.floor(seconds / 60);
        const sec = Math.floor(seconds % 60);
        return `${min}:${sec.toString().padStart(2, '0')}`;
    }

    toggleVideo() {
        if (this.isVideoPlaying) {
            this.bgVideo.pause();
            this.videoBtn.classList.remove('active');
        } else {
            this.bgVideo.play().catch(() => {});
            this.videoBtn.classList.add('active');
        }
        this.isVideoPlaying = !this.isVideoPlaying;
        this.videoBtn.innerHTML = this.getVideoIcon(this.isVideoPlaying);
    }

    toggleAudio() {
        if (this.isAudioPlaying) {
            this.bgAudio.pause();
            this.audioBtn.classList.remove('active');
        } else {
            this.bgAudio.play().catch(() => {});
            this.audioBtn.classList.add('active');
        }
        this.isAudioPlaying = !this.isAudioPlaying;
        this.audioBtn.innerHTML = this.getAudioIcon(this.isAudioPlaying);
    }

    getVideoIcon(isPlaying) {
        return isPlaying ?
            `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect><line x1="7" y1="2" x2="7" y2="22"></line><line x1="17" y1="2" x2="17" y2="22"></line></svg>` :
            `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="2" y1="2" x2="22" y2="22"></line><path d="M7 7v10c0 1.1.9 2 2 2h6c1.1 0 2-.9 2-2v-3l4 4V7l-4 4V7c0-1.1-.9-2-2-2H9c-1.1 0-2 .9-2 2z"></path></svg>`;
    }

    getAudioIcon(isPlaying) {
        return isPlaying ?
            `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>` :
            `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="2" y1="2" x2="22" y2="22"></line><path d="M9.34 9.34A4 4 0 0 0 6 13v3a4 4 0 0 0 4 4h4a4 4 0 0 0 4-4v-1"></path><path d="M12 9h1a4 4 0 0 1 4 4v1"></path><path d="M18 3L6 21"></path></svg>`;
    }
}

window.MediaController = MediaController;
document.addEventListener('DOMContentLoaded', () => {
    window.mediaController = new MediaController();
    window.mediaController.init();
});