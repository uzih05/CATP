// Result Page JavaScript
const API_BASE_URL = '';

// 10가지 적성 이름
const APTITUDE_NAMES = [
    '언어능력',
    '논리/분석력',
    '창의력',
    '사회성/공감능력',
    '주도성/리더십',
    '신체-활동성',
    '예술감각/공간지각',
    '체계성/꼼꼼함',
    '탐구심',
    '문제해결능력'
];

// 점수대별 등급 분류
function getGrade(percentage) {
    if (percentage >= 80) return { grade: 'excellent', label: '최적' };
    if (percentage >= 60) return { grade: 'good', label: '적합' };
    return { grade: 'potential', label: '관심요망' };
}

// State
let resultData = null;
let radarChart = null;

// DOM Elements
const personalityType = document.getElementById('personalityType');
const personalitySummary = document.getElementById('personalitySummary');
const strengthSummary = document.getElementById('strengthSummary');
const interestSummary = document.getElementById('interestSummary');
const topDepartmentSummary = document.getElementById('topDepartmentSummary');
const aptitudeLegend = document.getElementById('aptitudeLegend');
const topDepartments = document.getElementById('topDepartments');
const worstDepartments = document.getElementById('worstDepartments');
const similarDepartments = document.getElementById('similarDepartments');
const similarSection = document.getElementById('similarSection');
const similarSummaryText = document.getElementById('similarSummary');
const shareUrl = document.getElementById('shareUrl');
const copyBtn = document.getElementById('copyBtn');
const shareMessage = document.getElementById('shareMessage');
const interestTagsSection = document.getElementById('interestTagsSection');
const interestTagsContainer = document.getElementById('interestTagsContainer');

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    console.log('📄 결과 페이지 로드됨');

    const resultId = getResultIdFromUrl();
    console.log('🆔 Result ID:', resultId);

    if (!resultId) {
        console.error('❌ Result ID 없음');
        alert('잘못된 접근입니다.');
        window.location.href = '../index.html';
        return;
    }

    await loadResult(resultId);
    setupShareButton();
});

// Get result ID from URL
function getResultIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

// Load result from API
async function loadResult(resultId) {
    try {
        console.log('🔄 결과 로딩 시작:', resultId);

        const response = await fetch(`${API_BASE_URL}/api/results/${resultId}`);

        console.log('📥 응답 상태:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('❌ API 에러:', errorText);
            throw new Error(`결과를 불러오는데 실패했습니다 (${response.status})`);
        }

        resultData = await response.json();
        console.log('✅ 결과 로드 완료:', resultData);

        renderResult();

    } catch (error) {
        console.error('❌ 결과 로드 실패:', error);

        if (error.message.includes('Failed to fetch')) {
            alert('⚠️ 백엔드 서버에 연결할 수 없습니다.\n\n서버가 실행 중인지 확인해주세요.');
        } else {
            alert(`결과를 불러오는데 실패했습니다.\n\n에러: ${error.message}\n\n검사를 다시 시도해주세요.`);
        }

        setTimeout(() => {
            window.location.href = 'test.html';
        }, 5000);
    }
}

// Render all result sections
function renderResult() {
    renderPersonality();
    renderSummary();
    renderInterestTags();
    renderRadarChart();
    renderTopDepartments();
    renderSimilarDepartments();
    renderWorstDepartments();
    renderShareUrl();
}

// Render personality badge
function renderPersonality() {
    personalityType.textContent = resultData.personality;
}

// Render summary texts
function renderSummary() {
    const summary = resultData.summary;

    if (summary) {
        personalitySummary.textContent = summary.personality || '';
        strengthSummary.textContent = summary.strength || '';

        if (summary.interest) {
            interestSummary.textContent = summary.interest;
            interestSummary.style.display = 'block';
        } else {
            interestSummary.style.display = 'none';
        }

        // topDepartmentSummary 업데이트
        if (summary.top_department) {
            topDepartmentSummary.textContent = summary.top_department;
        }
    } else {
        // summary가 없는 경우 기본값
        personalitySummary.textContent = resultData.personality + ' 유형입니다.';
        strengthSummary.textContent = '';
        interestSummary.style.display = 'none';
    }
}

// Render interest tags
function renderInterestTags() {
    const tags = resultData.interest_tags;

    console.log('🏷️ Interest Tags:', tags);

    if (!tags || tags.length === 0) {
        console.log('⚠️ 관심사 태그가 없습니다');
        interestTagsSection.style.display = 'none';
        return;
    }

    interestTagsSection.style.display = 'block';
    interestTagsContainer.innerHTML = '';

    tags.forEach((tag, index) => {
        const tagElement = document.createElement('span');
        tagElement.className = 'interest-tag';
        tagElement.textContent = tag;
        tagElement.style.animationDelay = `${index * 0.05}s`;

        interestTagsContainer.appendChild(tagElement);
    });

    console.log('✅ 관심사 태그 렌더링 완료:', tags.length, '개');
}

// Render radar chart
function renderRadarChart() {
    const ctx = document.getElementById('radarChart').getContext('2d');

    if (radarChart) {
        radarChart.destroy();
    }

    const scores = resultData.scores;

    radarChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: APTITUDE_NAMES,
            datasets: [{
                label: '나의 적성',
                data: scores,
                fill: true,
                backgroundColor: 'rgba(102, 126, 234, 0.2)',
                borderColor: 'rgb(102, 126, 234)',
                pointBackgroundColor: 'rgb(102, 126, 234)',
                pointBorderColor: '#fff',
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderColor: 'rgb(102, 126, 234)'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                r: {
                    min: 0,
                    max: 5,
                    ticks: {
                        stepSize: 1,
                        font: { size: 12 }
                    },
                    pointLabels: {
                        font: { size: 13, weight: '600' }
                    }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return `${context.label}: ${context.parsed.r.toFixed(1)}점`;
                        }
                    }
                }
            }
        }
    });

    renderAptitudeLegend(scores);
}

// Render aptitude legend
function renderAptitudeLegend(scores) {
    aptitudeLegend.innerHTML = '';

    const colors = [
        '#667eea', '#764ba2', '#f093fb', '#4facfe',
        '#43e97b', '#fa709a', '#fee140', '#30cfd0',
        '#a8edea', '#fed6e3'
    ];

    scores.forEach((score, index) => {
        const item = document.createElement('div');
        item.className = 'legend-item';

        item.innerHTML = `
            <div class="legend-color" style="background: ${colors[index]}"></div>
            <span class="legend-text">${APTITUDE_NAMES[index]}</span>
            <span class="legend-score">${score.toFixed(1)}</span>
        `;

        aptitudeLegend.appendChild(item);
    });
}

// Render top departments
function renderTopDepartments() {
    const tops = resultData.top_departments;

    if (!tops || tops.length === 0) {
        topDepartments.innerHTML = '<p>추천 학과가 없습니다.</p>';
        return;
    }

    topDepartments.innerHTML = '';

    const medals = ['🥇', '🥈', '🥉'];

    tops.forEach((dept, index) => {
        const card = document.createElement('div');
        const gradeInfo = getGrade(dept.match_percentage);
        card.className = `department-card ${gradeInfo.grade}`;
        card.style.animationDelay = `${index * 0.1}s`;

        card.innerHTML = `
            <div class="department-rank">${medals[index] || (index + 1)}</div>
            <h3 class="department-name">${dept.department.name}</h3>
            <div class="department-match">
                <span class="match-percentage ${gradeInfo.grade}">${dept.match_percentage}%</span>
                <span class="match-label">일치</span>
                <span class="match-grade ${gradeInfo.grade}">${gradeInfo.label}</span>
            </div>
            <p class="department-reason">${dept.reason}</p>
            <a href="${dept.department.url}" target="_blank" rel="noopener noreferrer" class="department-link">
                학과 자세히 보기 →
            </a>
        `;

        topDepartments.appendChild(card);
    });
}

// Render similar departments
function renderSimilarDepartments() {
    const similars = resultData.similar_departments;

    if (!similars || similars.length === 0) {
        similarSection.style.display = 'none';
        return;
    }

    similarSection.style.display = 'block';

    const deptNames = similars.map(s => s.department.name);
    similarSummaryText.textContent = `관심 분야가 일치하는 학과: ${deptNames.join(', ')}`;

    similarDepartments.innerHTML = '';

    similars.forEach((dept, index) => {
        const card = document.createElement('div');
        card.className = 'similar-card';
        card.style.animationDelay = `${index * 0.1}s`;

        const tagsHtml = dept.common_tags
            ? dept.common_tags.map(tag => `<span class="tag">${tag}</span>`).join('')
            : '';

        card.innerHTML = `
            <h4 class="similar-name">${dept.department.name}</h4>
            <div class="similar-match">${dept.match_percentage}% 일치</div>
            <div class="similar-tags">${tagsHtml}</div>
            <a href="${dept.department.url}" target="_blank" rel="noopener noreferrer" class="department-link">
                학과 보기 →
            </a>
        `;

        similarDepartments.appendChild(card);
    });
}

// Render worst departments
function renderWorstDepartments() {
    const worsts = resultData.worst_departments;

    if (!worsts || worsts.length === 0) {
        worstDepartments.innerHTML = '<p>표시할 학과가 없습니다.</p>';
        return;
    }

    worstDepartments.innerHTML = '';

    worsts.forEach((dept, index) => {
        const card = document.createElement('div');
        card.className = 'worst-card';
        card.style.animationDelay = `${index * 0.1}s`;

        // mismatch_reason 필드 사용 (없으면 기본 메시지)
        const reason = dept.mismatch_reason || '적성이 맞지 않을 수 있습니다.';

        card.innerHTML = `
            <h4 class="worst-name">${dept.department.name}</h4>
            <div class="worst-percentage">${dept.match_percentage}% 일치</div>
            <p class="worst-reason">${reason}</p>
        `;

        worstDepartments.appendChild(card);
    });
}

// Render share URL
function renderShareUrl() {
    const url = `${window.location.origin}/pages/result.html?id=${resultData.id}`;
    shareUrl.value = url;
}

// Setup share button
function setupShareButton() {
    copyBtn.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(shareUrl.value);

            shareMessage.style.display = 'block';
            copyBtn.textContent = '✅ 복사 완료!';

            setTimeout(() => {
                shareMessage.style.display = 'none';
                copyBtn.textContent = '📋 링크 복사';
            }, 2000);

        } catch (error) {
            console.error('복사 실패:', error);

            shareUrl.select();
            document.execCommand('copy');

            shareMessage.style.display = 'block';
            shareMessage.textContent = '✅ 링크가 복사되었습니다!';

            setTimeout(() => {
                shareMessage.style.display = 'none';
            }, 2000);
        }
    });
}

// Smooth scroll for internal links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});