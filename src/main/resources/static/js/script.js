/* =================================
   CRAFTORA IMAGE SLIDER
================================= */

let slideIndex = 0;


/* Get all slides */

const slides = document.querySelectorAll(".slide");


/* Get all dots */

const dots = document.querySelectorAll(".dot");


/* =================================
   SHOW SLIDE
================================= */

function showSlide(index) {

    /* If index is greater than last slide */
    if (index >= slides.length) {
        slideIndex = 0;
    }


    /* If index is less than first slide */
    if (index < 0) {
        slideIndex = slides.length - 1;
    }


    /* Remove active from all slides */

    slides.forEach(function(slide) {
        slide.classList.remove("active");
    });


    /* Remove active from all dots */

    dots.forEach(function(dot) {
        dot.classList.remove("active");
    });


    /* Show current slide */

    slides[slideIndex].classList.add("active");


    /* Activate current dot */

    dots[slideIndex].classList.add("active");
}


/* =================================
   NEXT / PREVIOUS BUTTON
================================= */

function changeSlide(direction) {

    slideIndex = slideIndex + direction;

    showSlide(slideIndex);
}


/* =================================
   DOT BUTTON
================================= */

function currentSlide(index) {

    slideIndex = index;

    showSlide(slideIndex);
}


/* =================================
   AUTOMATIC SLIDER
================================= */

setInterval(function() {

    slideIndex++;

    showSlide(slideIndex);

}, 5000);

const featureCards = document.querySelectorAll(".feature-card");

const featureObserver = new IntersectionObserver(
    function(entries) {

        entries.forEach(function(entry) {

            if (entry.isIntersecting) {
                entry.target.classList.add("show");
            }

        });

    },
    {
        threshold: 0.2
    }
);

featureCards.forEach(function(card) {
    featureObserver.observe(card);
});

// Artisan cards animation

const artisanCards = document.querySelectorAll(".artisan-card");

const artisanObserver = new IntersectionObserver((entries) => {

    entries.forEach((entry) => {

        if (entry.isIntersecting) {
            entry.target.classList.add("show");
        }

    });

}, {
    threshold: 0.2
});

artisanCards.forEach((card) => {
    artisanObserver.observe(card);
});


// =================================
// NEWS SLIDER
// =================================

const newsTrack = document.getElementById("news-track");
const newsCards = document.querySelectorAll(".news-card");

let newsIndex = 0;

function showNextNews() {

    newsIndex++;

    if (newsIndex >= newsCards.length) {
        newsIndex = 0;
    }

    newsTrack.style.transform =
        "translateX(-" + (newsIndex * 100) + "%)";
}


// Change news every 4 seconds
setInterval(showNextNews, 4000);

// =================================
// CALENDAR
// =================================

const calendarMonth = document.getElementById("calendar-month");
const calendarYear = document.getElementById("calendar-year");
const calendarDays = document.getElementById("calendar-days");

let currentDate = new Date();

function generateCalendar() {

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    const monthName = currentDate.toLocaleString("default", {
        month: "long"
    });

    calendarMonth.textContent = monthName;
    calendarYear.textContent = year;

    calendarDays.innerHTML = "";

    // First day of the month
    const firstDay = new Date(year, month, 1).getDay();

    // Number of days in the month
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    // Empty spaces before first day
    for (let i = 0; i < firstDay; i++) {

        const emptyDay = document.createElement("span");

        calendarDays.appendChild(emptyDay);
    }

    // Create days
    for (let day = 1; day <= daysInMonth; day++) {

        const dayElement = document.createElement("span");

        dayElement.textContent = day;

        const today = new Date();

        // Highlight today's date
        if (
            day === today.getDate() &&
            month === today.getMonth() &&
            year === today.getFullYear()
        ) {

            dayElement.classList.add("today");
        }

        calendarDays.appendChild(dayElement);
    }
}


// Previous month
document.getElementById("prev-month").addEventListener("click", function () {

    currentDate.setMonth(currentDate.getMonth() - 1);

    generateCalendar();

});


// Next month
document.getElementById("next-month").addEventListener("click", function () {

    currentDate.setMonth(currentDate.getMonth() + 1);

    generateCalendar();

});


// Load calendar
generateCalendar();