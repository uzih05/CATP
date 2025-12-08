/**
 * Test Page JavaScript
 * 적성검사 질문 및 답변 처리
 */

const API_BASE_URL = '';

// 답변 옵션 (5점 리커트 척도)
const ANSWER_OPTIONS = [
    { value: 1, icon: '😞', label: '전혀 아니다' },
    { value: 2, icon: '😕', label: '아니다' },
    { value: 3, icon: '😐', label: '보통이다' },
    { value: 4, icon: '😊', label: '그렇다' },
    { value: 5, icon: '😄', label: '매우 그렇다' }
];

// 캐릭터 이미지 경로
const CHARACTER_IMAGES = {
    walk: [
        '../assets/images/character/walk-1.png',
        '../assets/images/character/walk-2.png',
    ],
    fall: '../assets/images/character/fall.png',
    yaho: '../assets/images/character/yaho.png'
};

// 캐릭터 설정
const CHARACTER_CONFIG = {
    idleTimeout: 5000,      // 5초 무응답 시 넘어짐
    yahoDisplayTime: 1000,  // 야호 표시 시간 1초
    walkFrameRate: 200      // 걷기 프레임 전환 속도 (ms)
};

// State
let questions = [];
let answers = [];
let currentIndex = 0;
let isSubmitting = false;

// 캐릭터 상태
let characterState = 'walk';  // 'walk' | 'fall' | 'yaho'
let idleTimer = null;
let walkAnimationTimer = null;
let currentWalkFrame = 0;

// DOM Elements
let questionCard;
let progressFill;
let progressText;
let progressPercent;
let prevBtn;
let nextBtn;
let loadingOverlay;
let progressCharacter;

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    // DOM 요소 캐싱
    questionCard = document.getElementById('questionCard');
    progressFill = document.getElementById('progressFill');
    progressText = document.getElementById('progressText');
    progressPercent = document.getElementById('progressPercent');
    prevBtn = document.getElementById('prevBtn');
    nextBtn = document.getElementById('nextBtn');
    loadingOverlay = document.getElementById('loadingOverlay');

    // 캐릭터 요소 생성
    createCharacterElement();

    // 질문 로드
    await loadQuestions();

    // 이벤트 바인딩
    setupEventListeners();

    // 첫 질문 표시
    if (questions.length > 0) {
        renderQuestion(currentIndex);
        updateProgress();
    }

    // 캐릭터 초기화
    startWalkAnimation();
    resetIdleTimer();
});

/**
 * 캐릭터 요소 생성
 */
function createCharacterElement() {
    progressCharacter = document.createElement('div');
    progressCharacter.className = 'progress-character walk';
    progressCharacter.id = 'progressCharacter';

    // progress-fill 안에 추가
    const progressFillEl = document.getElementById('progressFill');
    if (progressFillEl) {
        progressFillEl.appendChild(progressCharacter);
    }
}

/**
 * 캐릭터 상태 변경
 */
function setCharacterState(state) {
    characterState = state;
    progressCharacter.className = `progress-character ${state}`;

    // 걷기 애니메이션 관리
    if (state === 'walk') {
        startWalkAnimation();
    } else {
        stopWalkAnimation();

        if (state === 'fall') {
            progressCharacter.style.backgroundImage = `url('${CHARACTER_IMAGES.fall}')`;
        } else if (state === 'yaho') {
            progressCharacter.style.backgroundImage = `url('${CHARACTER_IMAGES.yaho}')`;
        }
    }
}

/**
 * 걷기 애니메이션 시작
 */
function startWalkAnimation() {
    stopWalkAnimation(); // 기존 애니메이션 정리

    currentWalkFrame = 0;
    updateWalkFrame();

    walkAnimationTimer = setInterval(() => {
        currentWalkFrame = (currentWalkFrame + 1) % CHARACTER_IMAGES.walk.length;
        updateWalkFrame();
    }, CHARACTER_CONFIG.walkFrameRate);
}

/**
 * 걷기 프레임 업데이트
 */
function updateWalkFrame() {
    if (characterState === 'walk' && progressCharacter) {
        progressCharacter.style.backgroundImage = `url('${CHARACTER_IMAGES.walk[currentWalkFrame]}')`;
    }
}

/**
 * 걷기 애니메이션 정지
 */
function stopWalkAnimation() {
    if (walkAnimationTimer) {
        clearInterval(walkAnimationTimer);
        walkAnimationTimer = null;
    }
}

/**
 * 무응답 타이머 리셋
 */
function resetIdleTimer() {
    clearTimeout(idleTimer);

    // 넘어진 상태였으면 다시 걷기로
    if (characterState === 'fall') {
        setCharacterState('walk');
    }

    idleTimer = setTimeout(() => {
        if (characterState === 'walk') {
            setCharacterState('fall');
        }
    }, CHARACTER_CONFIG.idleTimeout);
}

/**
 * 야호 애니메이션 실행
 */
function triggerYaho() {
    clearTimeout(idleTimer);
    setCharacterState('yaho');

    setTimeout(() => {
        setCharacterState('walk');
        resetIdleTimer();
    }, CHARACTER_CONFIG.yahoDisplayTime);
}

/**
 * API에서 질문 로드
 */
async function loadQuestions() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/questions`);
        if (!response.ok) throw new Error('Failed to load questions');

        const data = await response.json();
        questions = data.questions || [];
        answers = new Array(questions.length).fill(null);

        console.log(`✅ ${questions.length}개의 질문 로드 완료`);
    } catch (error) {
        console.error('❌ 질문 로드 실패:', error);
        showError('질문을 불러오는데 실패했습니다. 페이지를 새로고침 해주세요.');
    }
}

/**
 * 현재 질문 렌더링
 */
function renderQuestion(index) {
    const question = questions[index];
    if (!question) return;

    const savedAnswer = answers[index];

    // 애니메이션 처리
    questionCard.classList.remove('exit');
    questionCard.style.animation = 'none';
    questionCard.offsetHeight; // Reflow trigger
    questionCard.style.animation = 'slideInRight 0.4s ease forwards';

    questionCard.innerHTML = `
        <div class="question-header">
            <span class="question-number">QUESTION ${question.order || index + 1}</span>
        </div>
        <h2 class="question-text">${question.text}</h2>
        <div class="answer-options">
            ${ANSWER_OPTIONS.map(opt => `
                <button 
                    class="answer-btn ${savedAnswer === opt.value ? 'selected' : ''}"
                    data-value="${opt.value}"
                    type="button"
                >
                    <span class="answer-icon">${opt.icon}</span>
                    <span class="answer-label">${opt.label}</span>
                </button>
            `).join('')}
        </div>
    `;

    // 답변 버튼 이벤트 바인딩
    const answerBtns = questionCard.querySelectorAll('.answer-btn');
    answerBtns.forEach(btn => {
        btn.addEventListener('click', () => handleAnswer(parseInt(btn.dataset.value)));
    });

    // 이전 버튼 표시/숨김
    prevBtn.style.display = index > 0 ? 'flex' : 'none';

    // 다음 버튼 텍스트 업데이트
    updateNextButton();
}

/**
 * 답변 처리
 */
function handleAnswer(value) {
    // 답변 저장
    answers[currentIndex] = value;

    // UI 업데이트
    const answerBtns = questionCard.querySelectorAll('.answer-btn');
    answerBtns.forEach(btn => {
        btn.classList.toggle('selected', parseInt(btn.dataset.value) === value);
    });

    // 진행률 업데이트
    updateProgress();

    // 무응답 타이머 리셋
    resetIdleTimer();

    // 자동으로 다음 질문 (0.4초 딜레이)
    setTimeout(() => {
        if (currentIndex < questions.length - 1) {
            goToNext();
        } else {
            updateNextButton();
        }
    }, 400);
}

/**
 * 이전 질문으로
 */
function goToPrev() {
    if (currentIndex > 0) {
        resetIdleTimer();
        questionCard.classList.add('exit');
        setTimeout(() => {
            currentIndex--;
            renderQuestion(currentIndex);
            updateProgress();
        }, 300);
    }
}

/**
 * 다음 질문으로
 */
function goToNext() {
    if (answers[currentIndex] === null) {
        shakeCard();
        return;
    }

    if (currentIndex < questions.length - 1) {
        // 야호 애니메이션!
        triggerYaho();

        questionCard.classList.add('exit');
        setTimeout(() => {
            currentIndex++;
            renderQuestion(currentIndex);
            updateProgress();
        }, 300);
    } else {
        // 마지막 질문이면 제출
        submitTest();
    }
}

/**
 * 진행률 업데이트
 */
function updateProgress() {
    const answeredCount = answers.filter(a => a !== null).length;
    const percent = Math.round((answeredCount / questions.length) * 100);

    progressFill.style.width = `${percent}%`;
    progressText.textContent = `질문 ${currentIndex + 1} / ${questions.length}`;
    progressPercent.textContent = `${percent}%`;
}

/**
 * 다음 버튼 업데이트
 */
function updateNextButton() {
    const isLastQuestion = currentIndex === questions.length - 1;
    const hasCurrentAnswer = answers[currentIndex] !== null;
    const allAnswered = answers.every(a => a !== null);

    if (isLastQuestion && allAnswered) {
        nextBtn.innerHTML = `
            <span>결과 보기</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
        `;
        nextBtn.style.background = 'linear-gradient(135deg, #10b981, #059669)';
    } else {
        nextBtn.innerHTML = `
            <span>다음</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"/>
            </svg>
        `;
        nextBtn.style.background = '';
    }

    nextBtn.disabled = !hasCurrentAnswer;
}

/**
 * 카드 흔들기 애니메이션
 */
function shakeCard() {
    questionCard.style.animation = 'none';
    questionCard.offsetHeight;
    questionCard.style.animation = 'shake 0.5s ease';
}

/**
 * 테스트 제출
 */
async function submitTest() {
    // 미응답 체크
    const unansweredIndex = answers.findIndex(a => a === null);
    if (unansweredIndex !== -1) {
        currentIndex = unansweredIndex;
        renderQuestion(currentIndex);
        shakeCard();
        return;
    }

    if (isSubmitting) return;
    isSubmitting = true;

    // 걷기 애니메이션 정지
    stopWalkAnimation();

    // 로딩 표시
    loadingOverlay.classList.remove('hidden');

    try {
        const response = await fetch(`${API_BASE_URL}/api/results`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers: answers })
        });

        if (!response.ok) throw new Error('Submit failed');

        const result = await response.json();

        // 결과 페이지로 이동
        setTimeout(() => {
            window.location.href = `result.html?id=${result.id}`;
        }, 1000);

    } catch (error) {
        console.error('❌ 제출 실패:', error);
        loadingOverlay.classList.add('hidden');
        isSubmitting = false;
        startWalkAnimation(); // 다시 걷기 시작
        showError('결과 저장에 실패했습니다. 다시 시도해주세요.');
    }
}

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    prevBtn.addEventListener('click', goToPrev);
    nextBtn.addEventListener('click', goToNext);

    // 키보드 네비게이션
    document.addEventListener('keydown', (e) => {
        resetIdleTimer(); // 키보드 입력도 활동으로 인식

        if (e.key === 'ArrowLeft' && currentIndex > 0) {
            goToPrev();
        } else if (e.key === 'ArrowRight' && answers[currentIndex] !== null) {
            goToNext();
        } else if (e.key >= '1' && e.key <= '5') {
            handleAnswer(parseInt(e.key));
        }
    });

    // 마우스/터치 활동도 무응답 타이머 리셋
    document.addEventListener('mousemove', resetIdleTimer);
    document.addEventListener('touchstart', resetIdleTimer);
}

/**
 * 에러 표시
 */
function showError(message) {
    alert(message);
}

// 흔들기 애니메이션 추가
const style = document.createElement('style');
style.textContent = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
        20%, 40%, 60%, 80% { transform: translateX(5px); }
    }
`;
document.head.appendChild(style);
