/**
 * Media Controller
 * 배경 영상과 BGM 컨트롤
 */

class MediaController {
    constructor() {
        this.bgVideo = null;
        this.bgAudio = null;
        this.videoBtn = null;
        this.audioBtn = null;
        this.isVideoPlaying = true;
        this.isAudioPlaying = false;
        this.audioVolume = 0.3; // 기본 볼륨 30%
    }

    init() {
        this.createBackgroundVideo();
        this.createBackgroundAudio();
        this.createControls();
        this.bindEvents();
    }

    createBackgroundVideo() {
        // 비디오 배경 컨테이너
        const container = document.createElement('div');
        container.className = 'video-background';
        container.id = 'videoBackground';

        // 비디오 엘리먼트
        this.bgVideo = document.createElement('video');
        this.bgVideo.id = 'bgVideo';
        this.bgVideo.autoplay = true;
        this.bgVideo.muted = true;
        this.bgVideo.loop = true;
        this.bgVideo.playsInline = true;
        this.bgVideo.setAttribute('playsinline', '');
        
        // 비디오 소스 (여러 포맷 지원)
        this.bgVideo.innerHTML = `
            <source src="assets/videos/background.mp4" type="video/mp4">
            <source src="assets/videos/background.webm" type="video/webm">
        `;

        // 오버레이
        const overlay = document.createElement('div');
        overlay.className = 'overlay';

        container.appendChild(this.bgVideo);
        container.appendChild(overlay);
        document.body.insertBefore(container, document.body.firstChild);

        // 비디오 로드 실패 시 그라데이션 배경으로 대체
        this.bgVideo.addEventListener('error', () => {
            console.log('Video failed to load, using gradient background');
            container.classList.add('hidden');
            this.createGradientFallback();
        });

        // 비디오 재생 시작
        this.bgVideo.play().catch(() => {
            console.log('Autoplay blocked, using gradient background');
        });
    }

    createGradientFallback() {
        // 그라데이션 + Orbs 배경
        const gradientBg = document.createElement('div');
        gradientBg.className = 'gradient-background';
        gradientBg.id = 'gradientBackground';
        document.body.insertBefore(gradientBg, document.body.firstChild);

        // Floating Orbs
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
        
        // 오디오 소스
        this.bgAudio.innerHTML = `
            <source src="assets/audio/bgm.mp3" type="audio/mpeg">
            <source src="assets/audio/bgm.ogg" type="audio/ogg">
        `;

        document.body.appendChild(this.bgAudio);
    }

    createControls() {
        const controls = document.createElement('div');
        controls.className = 'media-controls';
        controls.id = 'mediaControls';

        // 영상 토글 버튼
        this.videoBtn = document.createElement('button');
        this.videoBtn.className = 'media-btn active';
        this.videoBtn.id = 'videoToggle';
        this.videoBtn.setAttribute('aria-label', '배경 영상 토글');
        this.videoBtn.innerHTML = this.getVideoIcon(true);

        // 음악 토글 버튼
        this.audioBtn = document.createElement('button');
        this.audioBtn.className = 'media-btn';
        this.audioBtn.id = 'audioToggle';
        this.audioBtn.setAttribute('aria-label', '배경 음악 토글');
        this.audioBtn.innerHTML = this.getAudioIcon(false);

        controls.appendChild(this.videoBtn);
        controls.appendChild(this.audioBtn);
        document.body.appendChild(controls);
    }

    bindEvents() {
        // 영상 토글
        this.videoBtn.addEventListener('click', () => {
            this.toggleVideo();
        });

        // 음악 토글
        this.audioBtn.addEventListener('click', () => {
            this.toggleAudio();
        });

        // 페이지 가시성 변경 시 (탭 전환)
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                // 탭 숨김 시 일시정지
                if (this.isAudioPlaying) {
                    this.bgAudio.pause();
                }
            } else {
                // 탭 복귀 시 재생
                if (this.isAudioPlaying) {
                    this.bgAudio.play().catch(() => {});
                }
            }
        });
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
            this.bgAudio.play().catch((e) => {
                console.log('Audio play failed:', e);
            });
            this.audioBtn.classList.add('active');
        }
        this.isAudioPlaying = !this.isAudioPlaying;
        this.audioBtn.innerHTML = this.getAudioIcon(this.isAudioPlaying);
    }

    setVolume(volume) {
        this.audioVolume = Math.max(0, Math.min(1, volume));
        if (this.bgAudio) {
            this.bgAudio.volume = this.audioVolume;
        }
    }

    getVideoIcon(isPlaying) {
        if (isPlaying) {
            // 비디오 재생 중 아이콘
            return `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect>
                    <line x1="7" y1="2" x2="7" y2="22"></line>
                    <line x1="17" y1="2" x2="17" y2="22"></line>
                    <line x1="2" y1="12" x2="22" y2="12"></line>
                    <line x1="2" y1="7" x2="7" y2="7"></line>
                    <line x1="2" y1="17" x2="7" y2="17"></line>
                    <line x1="17" y1="17" x2="22" y2="17"></line>
                    <line x1="17" y1="7" x2="22" y2="7"></line>
                </svg>
            `;
        } else {
            // 비디오 정지 아이콘
            return `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="2" y1="2" x2="22" y2="22"></line>
                    <path d="M7 7v10c0 1.1.9 2 2 2h6c1.1 0 2-.9 2-2v-3l4 4V7l-4 4V7c0-1.1-.9-2-2-2H9c-1.1 0-2 .9-2 2z"></path>
                </svg>
            `;
        }
    }

    getAudioIcon(isPlaying) {
        if (isPlaying) {
            // 음악 재생 중 아이콘
            return `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 18V5l12-2v13"></path>
                    <circle cx="6" cy="18" r="3"></circle>
                    <circle cx="18" cy="16" r="3"></circle>
                </svg>
            `;
        } else {
            // 음악 정지 아이콘
            return `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="2" y1="2" x2="22" y2="22"></line>
                    <path d="M9.34 9.34A4 4 0 0 0 6 13v3a4 4 0 0 0 4 4h4a4 4 0 0 0 4-4v-1"></path>
                    <path d="M12 9h1a4 4 0 0 1 4 4v1"></path>
                    <path d="M18 3L6 21"></path>
                </svg>
            `;
        }
    }
}

// 전역에서 사용 가능
window.MediaController = MediaController;

// DOM 로드 시 자동 초기화
document.addEventListener('DOMContentLoaded', () => {
    const mediaController = new MediaController();
    mediaController.init();
    window.mediaController = mediaController;
});
