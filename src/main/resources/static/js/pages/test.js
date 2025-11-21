// frontend/js/pages/test.js

const API_BASE_URL = '';
const QUESTIONS_PER_PAGE = 10; // 페이지당 질문 수는 유지

// State
let questions = [];
let answers = [];
let currentPage = 0;

// DOM Elements
const questionsList = document.getElementById('questionsList');
const nextPageBtn = document.getElementById('nextPageBtn');
const progressFill = document.getElementById('progressFill');
const progressPercent = document.getElementById('progressPercent');
const loadingOverlay = document.getElementById('loadingOverlay');

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    await loadQuestions();
    renderQuestionsPage();
    setupEventListeners();
});

// Load questions from API
async function loadQuestions() {
    try {
        questionsList.innerHTML = '<div style="text-align:center; color:white; font-size:1.2rem;">질문을 불러오는 중...</div>';

        const response = await fetch(`${API_BASE_URL}/api/questions`);
        if (!response.ok) throw new Error('API Error');

        const data = await response.json();
        questions = data.questions;

        if (!questions || questions.length === 0) {
            throw new Error('No Data');
        }

        // 이제 질문이 50개가 되어도 알아서 50칸짜리 배열이 만들어집니다.
        answers = new Array(questions.length).fill(null);
        console.log(`총 ${questions.length}개의 질문이 로드되었습니다.`);

    } catch (error) {
        console.error('질문 로드 실패:', error);
        alert('서버 연결에 실패했습니다. 백엔드를 실행해주세요.');
    }
}

// Render current page questions (10 at a time)
function renderQuestionsPage() {
    if (questions.length === 0) return;

    // Clear list
    questionsList.innerHTML = '';
    window.scrollTo(0, 0); // 맨 위로 스크롤

    const startIdx = currentPage * QUESTIONS_PER_PAGE;
    const endIdx = Math.min(startIdx + QUESTIONS_PER_PAGE, questions.length);
    const currentQuestions = questions.slice(startIdx, endIdx);

    // Update Progress
    updateProgress();

    // Update Button Text
    if (endIdx >= questions.length) {
        nextPageBtn.textContent = '결과 분석하기 🚀';
        nextPageBtn.classList.remove('btn-primary');
        nextPageBtn.classList.add('btn-accent'); // 강조 스타일 (css에 추가 필요하거나 primary 유지)
    } else {
        nextPageBtn.textContent = `다음 페이지 (${currentPage + 1}/${Math.ceil(questions.length / QUESTIONS_PER_PAGE)})`;
    }

    // Generate HTML for each question
    currentQuestions.forEach((question, index) => {
        const globalIndex = startIdx + index;
        const savedAnswer = answers[globalIndex];

        const card = document.createElement('div');
        card.className = 'question-card';
        card.id = `question-${globalIndex}`;
        if (savedAnswer !== null) card.classList.add('answered'); // 이미 답한 경우 흐리게

        card.innerHTML = `
            <div class="question-header">
                <span class="question-number">QUESTION ${question.order}</span>
            </div>
            <h2 class="question-text">${question.text}</h2>
            <div class="answer-options">
                ${generateAnswerButtons(globalIndex, savedAnswer)}
            </div>
        `;

        questionsList.appendChild(card);
    });
}

function generateAnswerButtons(questionIndex, savedAnswer) {
    const options = [
        { val: 1, icon: '😞', label: '전혀 아니다' },
        { val: 2, icon: '😐', label: '아니다' },
        { val: 3, icon: '😊', label: '보통이다' },
        { val: 4, icon: '😄', label: '그렇다' },
        { val: 5, icon: '😍', label: '매우 그렇다' }
    ];

    return options.map(opt => `
        <div class="answer-btn ${savedAnswer === opt.val ? 'selected' : ''}" 
             onclick="handleAnswerClick(${questionIndex}, ${opt.val}, this)">
            <span class="answer-icon">${opt.icon}</span>
            <span class="answer-label">${opt.label}</span>
        </div>
    `).join('');
}

// 전역 함수로 선언 (onclick attribute에서 사용)
window.handleAnswerClick = function(questionIndex, value, btnElement) {
    // 1. 답변 저장
    answers[questionIndex] = value;

    // 2. UI 업데이트 (버튼 선택 상태)
    const parentOptions = btnElement.parentElement;
    const buttons = parentOptions.querySelectorAll('.answer-btn');
    buttons.forEach(btn => btn.classList.remove('selected'));
    btnElement.classList.add('selected');

    // 3. 카드 스타일 변경 (흐리게 처리)
    const card = document.getElementById(`question-${questionIndex}`);
    card.classList.add('answered');

    // 4. 진행률 업데이트
    updateProgress();

    // 5. 다음 문제로 자동 스크롤
    // 현재 페이지의 마지막 문제가 아니면 다음 문제로 스크롤
    const relativeIndex = questionIndex % QUESTIONS_PER_PAGE;
    if (relativeIndex < QUESTIONS_PER_PAGE - 1) {
        const nextCardId = `question-${questionIndex + 1}`;
        const nextCard = document.getElementById(nextCardId);
        if (nextCard) {
            // 약간의 딜레이를 주어 시각적 피드백 확인 후 스크롤
            setTimeout(() => {
                nextCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 300);
        }
    } else {
        // 페이지의 마지막 문제인 경우, '다음 페이지' 버튼으로 스크롤 유도
        setTimeout(() => {
            nextPageBtn.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 300);
    }
};

function updateProgress() {
    const answeredCount = answers.filter(a => a !== null).length;
    const total = questions.length;
    const percent = Math.round((answeredCount / total) * 100);

    progressFill.style.width = `${percent}%`;
    progressPercent.textContent = `${percent}%`;
}

function setupEventListeners() {
    nextPageBtn.addEventListener('click', () => {
        const startIdx = currentPage * QUESTIONS_PER_PAGE;
        const endIdx = Math.min(startIdx + QUESTIONS_PER_PAGE, questions.length);

        // 현재 페이지의 모든 질문에 답했는지 확인
        for (let i = startIdx; i < endIdx; i++) {
            if (answers[i] === null) {
                alert(`${i + 1}번 질문에 답변해주세요! 🥺`);
                const card = document.getElementById(`question-${i}`);
                card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                card.classList.remove('answered'); // 강조를 위해 다시 밝게
                return;
            }
        }

        // 마지막 페이지라면 제출
        if (endIdx >= questions.length) {
            submitTest();
        } else {
            // 다음 페이지 로드
            currentPage++;
            renderQuestionsPage();
        }
    });
}

async function submitTest() {
    // 로딩 오버레이 표시 (로딩중임을 보여주고 싶어하셨던 부분)
    loadingOverlay.style.display = 'flex';

    try {
        const response = await fetch(`${API_BASE_URL}/api/results`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answers: answers })
        });

        if (!response.ok) throw new Error('제출 실패');

        const result = await response.json();

        // 잠시 로딩을 보여주기 위해 1초 딜레이 (선택사항)
        setTimeout(() => {
            window.location.href = `result.html?id=${result.id}`;
        }, 1000);

    } catch (error) {
        console.error(error);
        loadingOverlay.style.display = 'none';
        alert('결과 생성 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
}