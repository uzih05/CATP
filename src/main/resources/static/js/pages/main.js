// Main Page JavaScript

document.addEventListener('DOMContentLoaded', () => {
    // Smooth scroll for anchor links
    const anchorLinks = document.querySelectorAll('a[href^="#"]');

    anchorLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            const href = link.getAttribute('href');

            // Skip if it's just "#"
            if (href === '#') {
                e.preventDefault();
                return;
            }

            const target = document.querySelector(href);

            if (target) {
                e.preventDefault();

                const headerHeight = document.getElementById('header').offsetHeight;
                const targetPosition = target.getBoundingClientRect().top + window.pageYOffset - headerHeight;

                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });

    // Video fallback to image if video fails
    const video = document.querySelector('.hero-video');
    const heroBackground = document.querySelector('.hero-background');

    if (video) {
        video.addEventListener('error', () => {
            console.log('Video failed to load, using fallback image');
            video.style.display = 'none';

            // Create fallback image
            const img = document.createElement('img');
            img.className = 'hero-image';
            img.src = 'assets/images/bg-image.jpg';
            img.alt = 'Background';
            img.style.display = 'block';

            heroBackground.appendChild(img);
        });
    }

    // Parallax effect for hero section
    const heroSection = document.querySelector('.hero-section');
    const heroContent = document.querySelector('.hero-content');

    if (heroSection && heroContent) {
        window.addEventListener('scroll', () => {
            const scrolled = window.pageYOffset;
            const parallaxSpeed = 0.5;

            if (scrolled < window.innerHeight) {
                heroContent.style.transform = `translateY(${scrolled * parallaxSpeed}px)`;
                heroContent.style.opacity = 1 - (scrolled / window.innerHeight) * 0.8;
            }
        });
    }

    // Intersection Observer for feature cards animation
    const featureCards = document.querySelectorAll('.feature-card');

    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry, index) => {
            if (entry.isIntersecting) {
                setTimeout(() => {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateY(0)';
                }, index * 100);
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    featureCards.forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(30px)';
        card.style.transition = 'all 0.6s ease';
        observer.observe(card);
    });

    // Add ripple effect to glass buttons
    const glassButtons = document.querySelectorAll('.btn-glass');

    glassButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            // Create ripple element
            const ripple = document.createElement('span');
            ripple.classList.add('ripple');

            // Get button dimensions
            const rect = button.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);

            // Position ripple
            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = e.clientX - rect.left - size / 2 + 'px';
            ripple.style.top = e.clientY - rect.top - size / 2 + 'px';

            button.appendChild(ripple);

            // Remove ripple after animation
            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });
});

// Load animation for hero content
window.addEventListener('load', () => {
    const heroTitle = document.querySelector('.hero-title');
    const heroSubtitle = document.querySelector('.hero-subtitle');
    const heroActions = document.querySelector('.hero-actions');

    if (heroTitle) {
        heroTitle.style.animation = 'fadeInUp 1s ease';
    }
    if (heroSubtitle) {
        heroSubtitle.style.animation = 'fadeInUp 1s ease 0.2s both';
    }
    if (heroActions) {
        heroActions.style.animation = 'fadeInUp 1s ease 0.4s both';
    }
});