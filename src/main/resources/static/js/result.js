/**
 * Result Page JavaScript
 * 검사 결과 표시
 */

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

// 차트 색상
const CHART_COLORS = [
    '#6366f1', '#8b5cf6', '#a855f7', '#d946ef',
    '#ec4899', '#f43f5e', '#f97316', '#eab308',
    '#22c55e', '#14b8a6'
];

// State
let resultData = null;
let radarChart = null;

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    const resultId = getResultIdFromUrl();
    
    if (!resultId) {
        alert('잘못된 접근입니다.');
        window.location.href = '../index.html';
        return;
    }

    await loadResult(resultId);
});

/**
 * URL에서 결과 ID 추출
 */
function getResultIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}

/**
 * API에서 결과 로드
 */
async function loadResult(resultId) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/results/${resultId}`);
        
        if (!response.ok) {
            throw new Error('결과를 불러오는데 실패했습니다.');
        }

        resultData = await response.json();
        console.log('✅ 결과 로드 완료:', resultData);

        renderResult();

    } catch (error) {
        console.error('❌ 결과 로드 실패:', error);
        alert('결과를 불러오는데 실패했습니다. 검사를 다시 시도해주세요.');
        setTimeout(() => {
            window.location.href = 'test.html';
        }, 2000);
    }
}

/**
 * 전체 결과 렌더링
 */
function renderResult() {
    renderHeader();
    renderSummary();
    renderInterestTags();
    renderRadarChart();
    renderTopDepartments();
    renderSimilarDepartments();
    renderWorstDepartments();
    setupShareButton();
}

/**
 * 헤더 (성향 배지) 렌더링
 */
function renderHeader() {
    const personality = resultData.personality || '분석 중';
    const theme = getThemeFromPersonality(personality);
    
    document.getElementById('resultHeader').classList.add(`theme-${theme}`);
    document.getElementById('personalityType').textContent = personality;
    
    // 설명 텍스트
    const summary = resultData.summary;
    if (summary && summary.personality) {
        document.getElementById('personalityDescription').textContent = summary.personality;
    }
}

/**
 * 성향에 따른 테마 색상
 */
function getThemeFromPersonality(personality) {
    if (personality.includes('논리') || personality.includes('분석') || personality.includes('탐구')) {
        return 'indigo';
    } else if (personality.includes('창의') || personality.includes('예술')) {
        return 'purple';
    } else if (personality.includes('신체') || personality.includes('문제해결')) {
        return 'emerald';
    } else {
        return 'rose';
    }
}

/**
 * 요약 섹션 렌더링
 */
function renderSummary() {
    const summary = resultData.summary;
    if (!summary) return;

    let summaryHtml = '';
    
    if (summary.strength) {
        summaryHtml += `<p class="summary-text"><strong>💪 강점:</strong> ${summary.strength}</p>`;
    }
    
    if (summary.interest) {
        summaryHtml += `<p class="summary-text"><strong>🎯 관심사:</strong> ${summary.interest}</p>`;
    }
    
    if (summary.top_department) {
        summaryHtml += `<p class="summary-text"><strong>🎓 추천:</strong> ${summary.top_department}</p>`;
    }

    document.getElementById('summaryContent').innerHTML = summaryHtml;
}

/**
 * 관심사 태그 렌더링
 */
function renderInterestTags() {
    const tags = resultData.interest_tags;
    const container = document.getElementById('interestTags');
    
    if (!tags || tags.length === 0) {
        container.parentElement.style.display = 'none';
        return;
    }

    container.innerHTML = tags.map(tag => 
        `<span class="interest-tag">${tag}</span>`
    ).join('');
}

/**
 * 레이더 차트 렌더링
 */
function renderRadarChart() {
    const ctx = document.getElementById('radarChart').getContext('2d');
    const scores = resultData.scores;

    radarChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: APTITUDE_NAMES,
            datasets: [{
                label: '나의 적성',
                data: scores,
                fill: true,
                backgroundColor: 'rgba(99, 102, 241, 0.2)',
                borderColor: 'rgba(99, 102, 241, 1)',
                borderWidth: 2,
                pointBackgroundColor: 'rgba(99, 102, 241, 1)',
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointRadius: 4,
                pointHoverRadius: 6
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
                        font: { size: 11 },
                        backdropColor: 'transparent'
                    },
                    pointLabels: {
                        font: { size: 11, weight: '600' },
                        color: '#374151'
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.1)'
                    },
                    angleLines: {
                        color: 'rgba(0, 0, 0, 0.1)'
                    }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: (context) => `${context.label}: ${context.parsed.r.toFixed(1)}점`
                    }
                }
            }
        }
    });

    renderAptitudeLegend(scores);
}

/**
 * 적성 범례 렌더링
 */
function renderAptitudeLegend(scores) {
    const container = document.getElementById('aptitudeLegend');

    container.innerHTML = scores.map((score, index) => `
        <div class="legend-item">
            <div class="legend-color" style="background: ${CHART_COLORS[index]}"></div>
            <span class="legend-name">${APTITUDE_NAMES[index]}</span>
            <span class="legend-score">${score.toFixed(1)}</span>
        </div>
    `).join('');
}

/**
 * 학과명으로 마스코트 이미지 경로 생성
 * 파일명: 학과명.png (예: 컴퓨터공학과.png)
 */
function getMascotImagePath(departmentName) {
    return `../assets/images/mascot/${departmentName}.png`;
}

/**
 * 마스코트 이미지 로드 실패 시 기본 이미지로 대체
 */
function handleMascotError(img) {
    img.onerror = null; // 무한 루프 방지
    img.src = '../assets/images/mascot/default.png';
}

/**
 * 추천 학과 Top 3 렌더링
 */
function renderTopDepartments() {
    const departments = resultData.top_departments;
    const container = document.getElementById('topDepartments');

    if (!departments || departments.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: var(--gray-500);">추천 학과가 없습니다.</p>';
        return;
    }

    container.innerHTML = departments.map((dept, index) => {
        const deptName = dept.department.name;
        const mascotPath = getMascotImagePath(deptName);
        
        return `
            <div class="department-card glass-card">
                <div class="department-rank">
                    <img src="${mascotPath}" alt="${deptName} 마스코트" onerror="handleMascotError(this)">
                </div>
                <h4 class="department-name">${deptName}</h4>
                <div class="department-match">
                    <span class="match-percentage">${dept.match_percentage}%</span>
                    <span class="match-label">일치</span>
                </div>
                <p class="department-reason">${dept.reason || '적성이 잘 맞습니다.'}</p>
                <a href="${dept.department.url}" target="_blank" rel="noopener noreferrer" class="department-link">
                    학과 자세히 보기 →
                </a>
            </div>
        `;
    }).join('');
}

/**
 * 관심사 기반 추천 학과 렌더링
 */
function renderSimilarDepartments() {
    const departments = resultData.similar_departments;
    const section = document.getElementById('similarSection');
    const container = document.getElementById('similarDepartments');

    if (!departments || departments.length === 0) {
        section.style.display = 'none';
        return;
    }

    section.style.display = 'block';

    container.innerHTML = departments.map(dept => `
        <div class="similar-card glass-card-light">
            <h4 class="similar-name">${dept.department.name}</h4>
            <div class="similar-match">${dept.match_percentage}% 일치</div>
            ${dept.common_tags ? `
                <div class="common-tags">
                    ${dept.common_tags.slice(0, 3).map(tag => `<span class="common-tag">${tag}</span>`).join('')}
                </div>
            ` : ''}
            <a href="${dept.department.url}" target="_blank" rel="noopener noreferrer" class="department-link">
                학과 보기 →
            </a>
        </div>
    `).join('');
}

/**
 * 비추천 학과 렌더링
 */
function renderWorstDepartments() {
    const departments = resultData.worst_departments;
    const container = document.getElementById('worstDepartments');

    if (!departments || departments.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: var(--gray-500);">표시할 학과가 없습니다.</p>';
        return;
    }

    container.innerHTML = departments.map(dept => `
        <div class="worst-card glass-card-light">
            <h4 class="worst-name">${dept.department.name}</h4>
            <div class="worst-percentage">${dept.match_percentage}%</div>
            <p class="worst-reason">${dept.mismatch_reason || '적성이 맞지 않을 수 있습니다.'}</p>
        </div>
    `).join('');
}

/**
 * 공유 버튼 설정
 */
function setupShareButton() {
    const shareInput = document.getElementById('shareUrl');
    const copyBtn = document.getElementById('copyBtn');
    const shareMessage = document.getElementById('shareMessage');

    // URL 설정
    const shareUrl = `${window.location.origin}/pages/result.html?id=${resultData.id}`;
    shareInput.value = shareUrl;

    // 복사 버튼
    copyBtn.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(shareUrl);
            shareMessage.textContent = '✅ 링크가 복사되었습니다!';
            shareMessage.style.display = 'block';
            copyBtn.textContent = '✅ 복사 완료!';

            setTimeout(() => {
                shareMessage.style.display = 'none';
                copyBtn.textContent = '📋 복사';
            }, 2000);
        } catch (error) {
            shareInput.select();
            document.execCommand('copy');
            shareMessage.textContent = '✅ 링크가 복사되었습니다!';
            shareMessage.style.display = 'block';
        }
    });
}

/**
 * 다시 검사하기
 */
function restartTest() {
    window.location.href = 'test.html';
}

/**
 * 홈으로
 */
function goHome() {
    window.location.href = '../index.html';
}
