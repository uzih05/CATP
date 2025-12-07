/**
 * Cherry Blossoms Animation
 * 벚꽃 꽃잎이 떨어지는 효과
 */

class CherryBlossoms {
    constructor(options = {}) {
        this.container = null;
        this.petalCount = options.petalCount || 25;
        this.colors = options.colors || ['#FFC0CB', '#FFB6C1', '#FF69B4', '#FFD1DC'];
        this.minSize = options.minSize || 10;
        this.maxSize = options.maxSize || 20;
        this.minDuration = options.minDuration || 10;
        this.maxDuration = options.maxDuration || 20;
    }

    init() {
        // 컨테이너 생성
        this.container = document.createElement('div');
        this.container.className = 'cherry-blossoms';
        document.body.appendChild(this.container);

        // 꽃잎 생성
        for (let i = 0; i < this.petalCount; i++) {
            this.createPetal(i);
        }
    }

    createPetal(index) {
        const petal = document.createElement('div');
        petal.className = 'petal';

        // 랜덤 속성
        const size = this.minSize + Math.random() * (this.maxSize - this.minSize);
        const left = Math.random() * 100;
        const delay = Math.random() * 10;
        const duration = this.minDuration + Math.random() * (this.maxDuration - this.minDuration);
        const color = this.colors[Math.floor(Math.random() * this.colors.length)];

        // 스타일 적용
        petal.style.cssText = `
            left: ${left}%;
            width: ${size}px;
            height: ${size}px;
            animation-delay: ${delay}s;
            animation-duration: ${duration}s;
        `;

        // SVG 꽃잎 모양
        petal.innerHTML = this.getPetalSVG(color);

        this.container.appendChild(petal);
    }

    getPetalSVG(color) {
        const opacity = 0.6 + Math.random() * 0.3;
        return `
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                    d="M12 2C10 4 8 6 8 9C8 10.5 9 11.5 10 12C9 12.5 8 13.5 8 15C8 18 10 20 12 22C14 20 16 18 16 15C16 13.5 15 12.5 14 12C15 11.5 16 10.5 16 9C16 6 14 4 12 2Z"
                    fill="${color}"
                    opacity="${opacity}"
                />
                <path
                    d="M12 12C10.5 11 9.5 10 9 9C9.5 8 10.5 7 12 6C13.5 7 14.5 8 15 9C14.5 10 13.5 11 12 12Z"
                    fill="${color}"
                    opacity="${opacity * 0.7}"
                />
            </svg>
        `;
    }

    destroy() {
        if (this.container) {
            this.container.remove();
            this.container = null;
        }
    }
}

// 전역에서 사용 가능하도록
window.CherryBlossoms = CherryBlossoms;

// DOM 로드 시 자동 초기화
document.addEventListener('DOMContentLoaded', () => {
    const cherryBlossoms = new CherryBlossoms({
        petalCount: 25,
        minSize: 10,
        maxSize: 22,
        minDuration: 12,
        maxDuration: 22
    });
    cherryBlossoms.init();
});
