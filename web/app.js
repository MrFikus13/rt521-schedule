/* ============================================================
   RT-521 PWA — data, MIUIX and iOS Liquid Glass UI
   ============================================================ */
(() => {
  'use strict';

  const DAY_KEYS = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
  const DAY_NAMES = {
    monday: 'Понедельник', tuesday: 'Вторник', wednesday: 'Среда',
    thursday: 'Четверг', friday: 'Пятница', saturday: 'Суббота', sunday: 'Воскресенье',
  };
  const DAY_SHORT = {
    monday: 'Пн', tuesday: 'Вт', wednesday: 'Ср',
    thursday: 'Чт', friday: 'Пт', saturday: 'Сб', sunday: 'Вс',
  };
  const MONTHS_GEN = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня',
    'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
  const MONTHS_SHORT = ['янв', 'фев', 'мар', 'апр', 'мая', 'июн',
    'июл', 'авг', 'сен', 'окт', 'ноя', 'дек'];

  const SLOTS = [
    { n: 1, start: 480, end: 580 },
    { n: 2, start: 595, end: 695 },
    { n: 3, start: 735, end: 835 },
    { n: 4, start: 850, end: 950 },
    { n: 5, start: 980, end: 1080 },
    { n: 6, start: 1095, end: 1195 },
  ];
  const BREAKS = [
    { start: 580, end: 595, big: false },
    { start: 695, end: 735, big: true },
    { start: 835, end: 850, big: false },
    { start: 950, end: 980, big: true },
    { start: 1080, end: 1095, big: false },
  ];

  const SUBJECT_COLORS = {
    'Программирование': '#4285f4',
    'Физическая культура и здоровье': '#2e9e5b',
    'Радиоприёмные устройства': '#f9a825',
    'Основы построения систем и сетей телекоммуникаций': '#9c4df4',
    'Теория электрической связи': '#e0457b',
    'Цифровые микропроцессорные устройства': '#00897b',
    'Метрология и специальные измерения': '#f57c00',
    'Направляющие системы телекоммуникаций': '#5c6bc0',
    'Радиопередающие устройства': '#e64a45',
    'Защита населения и территорий от чрезвычайных ситуаций': '#8d6e63',
  };
  const SUBJECT_ABBR = {
    'Программирование': 'Пр',
    'Физическая культура и здоровье': 'ФК',
    'Радиоприёмные устройства': 'РП',
    'Основы построения систем и сетей телекоммуникаций': 'ОС',
    'Теория электрической связи': 'ТЭ',
    'Цифровые микропроцессорные устройства': 'ЦМ',
    'Метрология и специальные измерения': 'МС',
    'Направляющие системы телекоммуникаций': 'НС',
    'Радиопередающие устройства': 'РПд',
    'Защита населения и территорий от чрезвычайных ситуаций': 'ЗН',
  };

  const STORAGE = 'rt521_web_v2';
  const SETTINGS = 'rt521_web_settings_v2';

  const $ = (s, root = document) => root.querySelector(s);
  const $$ = (s, root = document) => [...root.querySelectorAll(s)];
  const icon = (name, cls = 'icon') => `<svg class="${cls}" aria-hidden="true"><use href="#i-${name}"></use></svg>`;

  const state = {
    schedule: null,
    view: 'day',
    selectedDate: stripTime(new Date()),
    weekBase: mondayOf(stripTime(new Date())),
    settings: loadSettings(),
    lastFetch: Number(readStorage(`${STORAGE}_last_fetch`) || 0),
    loading: true,
    refreshing: false,
    error: '',
    expanded: new Set([iso(new Date())]),
    lastStatusKey: '',
    autoTimer: 0,
  };

  /* ============================================================
     Small utils
     ============================================================ */

  function stripTime(d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }
  function iso(d) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  function parseISO(value) {
    const [y, m, d] = value.split('-').map(Number);
    return new Date(y, m - 1, d);
  }
  function addDays(d, n) {
    const copy = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    copy.setDate(copy.getDate() + n);
    return copy;
  }
  function mondayOf(d) {
    const copy = stripTime(d);
    const day = copy.getDay();
    copy.setDate(copy.getDate() - (day === 0 ? 6 : day - 1));
    return copy;
  }
  function dateText(d, withDay = true) {
    const base = `${d.getDate()} ${MONTHS_GEN[d.getMonth()]}`;
    return withDay ? `${DAY_NAMES[DAY_KEYS[d.getDay()]]}, ${base}` : base;
  }
  function shortDate(d) {
    return `${DAY_SHORT[DAY_KEYS[d.getDay()]]}, ${d.getDate()} ${MONTHS_SHORT[d.getMonth()]}`;
  }
  function parseTime(s) {
    const [h, m] = s.split(':').map(Number);
    return h * 60 + m;
  }
  function fmtTime(min) {
    return `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`;
  }
  function fmtCountdown(seconds) {
    const s = Math.max(0, Math.round(seconds));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
      : `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  }
  function pluralPairs(n) {
    const m10 = n % 10, m100 = n % 100;
    if (m10 === 1 && m100 !== 11) return `${n} пара`;
    if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return `${n} пары`;
    return `${n} пар`;
  }
  function pluralDays(n) {
    const m10 = n % 10, m100 = n % 100;
    if (m10 === 1 && m100 !== 11) return 'день';
    if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return 'дня';
    return 'дней';
  }
  function pluralWorks(n) {
    const m10 = n % 10, m100 = n % 100;
    if (m10 === 1 && m100 !== 11) return 'работа';
    if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return 'работы';
    return 'работ';
  }
  function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
    }[c]));
  }
  function baseSubject(subject) {
    return String(subject || '').replace(/\s*\([^)]*\)\s*/g, ' ').replace(/\s+/g, ' ').trim();
  }
  function normSubject(subject) {
    return baseSubject(subject).toLowerCase().replace(/ё/g, 'е');
  }
  function subjectColor(subject) {
    return SUBJECT_COLORS[baseSubject(subject)] || 'var(--accent)';
  }
  function abbr(subject) {
    return SUBJECT_ABBR[baseSubject(subject)] || baseSubject(subject).slice(0, 2);
  }
  function semanticChange(kind) {
    if (kind === 'cancel' || kind === 'отмена') return 'cancel';
    if (kind === 'extra' || kind === 'add' || kind === 'доп') return 'extra';
    if (kind === 'note' || kind === 'info' || kind === 'примечание') return 'note';
    return 'replace';
  }

  function readStorage(key) {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }
  function writeStorage(key, value) {
    try {
      localStorage.setItem(key, value);
    } catch {
      /* private mode or storage full: keep working in memory */
    }
  }
  function loadSettings() {
    const defaults = { style: 'miuix', mode: 'auto', parityOverride: 0, installDismissed: false };
    try {
      return { ...defaults, ...JSON.parse(readStorage(SETTINGS) || '{}') };
    } catch {
      return defaults;
    }
  }
  function saveSettings() {
    writeStorage(SETTINGS, JSON.stringify(state.settings));
  }

  /* ============================================================
     Schedule math (mirrors the Android app)
     ============================================================ */

  function semesterWeek(date) {
    if (!state.schedule) return 1;
    const start = mondayOf(parseISO(state.schedule.semester_start));
    const diff = Math.round((stripTime(date) - start) / 86400000);
    return Math.floor(diff / 7) + 1;
  }
  function autoParity(date) {
    return semesterWeek(date) % 2 === 1 ? 'odd' : 'even';
  }
  function parityFor(date) {
    if (state.settings.parityOverride === 1) return 'odd';
    if (state.settings.parityOverride === 2) return 'even';
    return autoParity(date);
  }
  function parityLabel(p) { return p === 'odd' ? 'нечётная' : 'чётная'; }

  function matchesWeeks(lesson, parity) {
    if (lesson.weeks === 'all' || !lesson.weeks) return true;
    if (!Array.isArray(lesson.weeks)) return true;
    return lesson.weeks.some(w => w % 2 === (parity === 'odd' ? 1 : 0));
  }

  function cloneLesson(lesson, extra = {}) {
    return {
      time: lesson.time,
      subject: lesson.subject,
      room: lesson.room || '',
      teachers: Array.isArray(lesson.teachers) ? lesson.teachers : [],
      weeks: lesson.weeks === 'all' ? 'all' : lesson.weeks,
      change: null,
      note: '',
      cancelled: false,
      start: parseTime(lesson.time.split('-')[0]),
      end: parseTime(lesson.time.split('-')[1]),
      ...extra,
    };
  }

  function lessonsFor(date, parity = parityFor(date)) {
    if (!state.schedule) return [];
    const key = DAY_KEYS[date.getDay()];
    const base = (state.schedule.schedule[key] || []).filter(l => matchesWeeks(l, parity));
    const list = base.map(l => cloneLesson(l));
    const dateISO = iso(date);
    const changes = (state.schedule.changes || [])
      .filter(c => c.date === dateISO)
      .sort((a, b) => {
        const order = { cancel: 0, replace: 1, extra: 2, note: 3 };
        const ka = semanticChange(a.kind), kb = semanticChange(b.kind);
        return (order[ka] - order[kb]) || (parseTime(a.time.split('-')[0]) - parseTime(b.time.split('-')[0]));
      });

    changes.forEach(change => {
      const kind = semanticChange(change.kind);
      const time = change.time;
      const subject = change.subject || '';
      if (kind === 'cancel') {
        const matching = list.filter(l => l.time === time && (
          !subject || normSubject(l.subject) === normSubject(subject)
        ));
        if (!matching.length) {
          list.push(cloneLesson({
            time,
            subject: subject || 'Пара',
            room: change.room || '',
            teachers: change.teachers || [],
          }, { change: 'cancel', note: change.note || '', cancelled: true }));
        } else {
          matching.forEach(l => {
            l.change = 'cancel';
            l.cancelled = true;
            if (change.note) l.note = l.note ? `${l.note} · ${change.note}` : change.note;
          });
        }
      } else if (kind === 'replace') {
        for (let i = list.length - 1; i >= 0; i--) if (list[i].time === time) list.splice(i, 1);
        list.push(cloneLesson({
          time,
          subject: subject || 'Пара',
          room: change.room || '',
          teachers: change.teachers || [],
        }, { change: 'replace', note: change.note || '' }));
      } else if (kind === 'extra') {
        list.push(cloneLesson({
          time,
          subject: subject || 'Пара',
          room: change.room || '',
          teachers: change.teachers || [],
        }, { change: 'extra', note: change.note || '' }));
      } else {
        const matching = list.filter(l => l.time === time && (
          !subject || normSubject(l.subject) === normSubject(subject)
        ));
        if (!matching.length) {
          list.push(cloneLesson({
            time,
            subject: subject || 'Примечание',
            room: change.room || '',
            teachers: change.teachers || [],
          }, { change: 'note', note: change.note || '' }));
        } else {
          matching.forEach(l => {
            if (!l.change) l.change = 'note';
            l.note = l.note ? `${l.note} · ${change.note}` : (change.note || '');
          });
        }
      }
    });

    return list.sort((a, b) => a.start - b.start || a.end - b.end);
  }

  function activeLessons(lessons) { return lessons.filter(l => !l.cancelled); }

  function liveStatus(now, lessons) {
    const active = activeLessons(lessons);
    if (!active.length) return { kind: 'none', totalToday: 0 };
    const t = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60;
    const sorted = [...active].sort((a, b) => a.start - b.start);
    for (const lesson of sorted) {
      const start = lesson.start, end = lesson.end;
      const midStart = start + 45, midEnd = start + 55;
      if (t >= start && t < end) {
        let phase, phaseEnd, total;
        if (t < midStart) { phase = 'first'; phaseEnd = midStart; total = 45; }
        else if (t < midEnd) { phase = 'mid'; phaseEnd = midEnd; total = 10; }
        else { phase = 'second'; phaseEnd = end; total = 45; }
        return {
          kind: 'lesson', lesson, phase,
          endMinutes: phaseEnd, totalMinutes: total,
          secondsLeft: Math.max(0, Math.round((phaseEnd - t) * 60)),
          progress: Math.min(1, Math.max(0, (t - start) / (end - start))),
        };
      }
    }
    const first = sorted[0];
    if (t < first.start) {
      return {
        kind: 'before', lesson: first,
        secondsLeft: Math.max(0, Math.round((first.start - t) * 60)),
      };
    }
    for (let i = 0; i < sorted.length; i++) {
      const current = sorted[i], next = sorted[i + 1];
      if (t >= current.end && (!next || t < next.start)) {
        if (!next) return { kind: 'finished', totalToday: sorted.length };
        const gap = next.start - current.end;
        return {
          kind: 'break', next, big: gap >= 25,
          endMinutes: next.start, totalMinutes: gap,
          secondsLeft: Math.max(0, Math.round((next.start - t) * 60)),
          progress: Math.min(1, Math.max(0, (t - current.end) / gap)),
        };
      }
    }
    return { kind: 'finished', totalToday: sorted.length };
  }

  function worksFor(date, subject, today = stripTime(new Date())) {
    if (!state.schedule) return [];
    const ns = normSubject(subject);
    return (state.schedule.works || []).filter(work => {
      if (normSubject(work.subject) !== ns) return false;
      const workDate = parseISO(work.date);
      if (workDate < today || workDate < date) return false;
      const days = Math.round((workDate - stripTime(date)) / 86400000);
      return days >= 0 && days <= 7;
    }).sort((a, b) => a.date.localeCompare(b.date));
  }
  function worksOn(date) {
    return (state.schedule?.works || []).filter(w => w.date === iso(date));
  }
  function worksBetween(from, to) {
    return (state.schedule?.works || []).filter(w => {
      const d = parseISO(w.date);
      return d >= from && d <= to;
    }).sort((a, b) => a.date.localeCompare(b.date));
  }

  function nextLessonAfter(lessons, current) {
    const active = activeLessons(lessons).sort((a, b) => a.start - b.start);
    if (!current) return active[0] || null;
    return active.find(l => l.start >= current.end) || null;
  }

  /* ============================================================
     Theme
     ============================================================ */

  let mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  function resolvedDark() {
    if (state.settings.mode === 'dark') return true;
    if (state.settings.mode === 'light') return false;
    return mediaQuery.matches;
  }
  function applyTheme() {
    const root = document.documentElement;
    root.dataset.style = state.settings.style;
    root.dataset.dark = resolvedDark() ? 'true' : 'false';
    const meta = $('meta[name="theme-color"]');
    if (meta) {
      const liquid = state.settings.style === 'liquid';
      meta.content = liquid
        ? (resolvedDark() ? '#10101d' : '#e9e6ff')
        : (resolvedDark() ? '#000000' : '#f7f7f7');
    }
  }
  mediaQuery.addEventListener?.('change', () => {
    if (state.settings.mode === 'auto') {
      applyTheme();
      render();
    }
  });

  /* ============================================================
     Rendering
     ============================================================ */

  function render() {
    applyTheme();
    const schedule = state.schedule;
    const group = schedule?.group || 'РТ-521';
    const semester = schedule?.semester || 5;
    $('#groupLabel').textContent = `${group} · ${semester} семестр`;
    const title = $('#pageTitle');
    const subtitle = $('#pageSubtitle');
    if (state.view === 'settings') {
      title.textContent = 'Настройки';
      subtitle.textContent = 'Оформление и обновления';
    } else if (state.view === 'week') {
      title.textContent = 'Неделя';
      subtitle.textContent = `Обзор недели · ${schedule?.academic_year || ''}`;
    } else {
      title.textContent = 'Расписание';
      subtitle.textContent = `${dateText(state.selectedDate)} · ${schedule?.academic_year || ''}`;
    }
    $$('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.view === state.view));
    const content = $('#content');
    if (!schedule) {
      content.innerHTML = state.error
        ? `<div class="update-error">${icon('warning')}<span>${esc(state.error)}</span><button data-refresh>Повторить</button></div>`
        : `<div class="loading-card"><span class="loading-ring"></span><p>Загружаю расписание…</p></div>`;
      return;
    }
    if (state.view === 'week') content.innerHTML = renderWeek();
    else if (state.view === 'settings') content.innerHTML = renderSettings();
    else content.innerHTML = renderDay();
    requestAnimationFrame(() => {
      content.scrollTop = 0;
    });
  }

  function weekNavHTML(monday, { todayButton = true } = {}) {
    const today = stripTime(new Date());
    const currentMonday = mondayOf(today);
    const week = semesterWeek(monday);
    return `
      <div class="week-head">
        <button class="week-arrow" data-prev-week aria-label="Предыдущая неделя">${icon('chevron-left')}</button>
        <div class="week-head-center">
          <div class="week-range">${weekRangeText(monday)}</div>
          <div class="week-meta">${week}-я неделя · ${parityLabel(parityFor(monday))}</div>
        </div>
        <button class="week-arrow" data-next-week aria-label="Следующая неделя">${icon('chevron-right')}</button>
      </div>
      ${todayButton && (monday.getTime() !== currentMonday.getTime() || state.selectedDate.getTime() !== today.getTime())
        ? `<div style="text-align:right"><button class="today-link" data-today>Сегодня</button></div>`
        : ''}`;
  }

  function weekRangeText(monday) {
    const sunday = addDays(monday, 6);
    if (monday.getMonth() === sunday.getMonth()) {
      return `${monday.getDate()} – ${sunday.getDate()} ${MONTHS_GEN[monday.getMonth()]}`;
    }
    return `${monday.getDate()} ${MONTHS_SHORT[monday.getMonth()]} – ${sunday.getDate()} ${MONTHS_SHORT[sunday.getMonth()]}`;
  }

  function dateStripHTML() {
    const today = stripTime(new Date());
    const monday = mondayOf(state.selectedDate);
    const works = state.schedule?.works || [];
    return `<div class="date-strip">${Array.from({ length: 7 }, (_, i) => {
      const date = addDays(monday, i);
      const selected = date.getTime() === state.selectedDate.getTime();
      const isToday = date.getTime() === today.getTime();
      const hasWork = works.some(w => w.date === iso(date));
      return `<button class="date-cell${selected ? ' is-selected' : ''}${isToday ? ' is-today' : ''}${hasWork ? ' has-work' : ''}" data-date="${iso(date)}">
        <span class="dow">${DAY_SHORT[DAY_KEYS[date.getDay()]]}</span>
        <span class="num">${date.getDate()}</span>
        <span class="dot"></span>
      </button>`;
    }).join('')}</div>`;
  }

  function renderDay() {
    const date = state.selectedDate;
    const today = stripTime(new Date());
    const parity = parityFor(date);
    const lessons = lessonsFor(date, parity);
    const active = activeLessons(lessons);
    const live = liveStatus(new Date(), lessons);
    const works = worksOn(date);
    const monday = mondayOf(date);
    state.lastStatusKey = statusKey(live);
    const error = state.error
      ? `<div class="update-error">${icon('warning')}<span>${esc(state.error)}</span><button data-refresh>Повторить</button></div>`
      : '';
    let list = '';
    if (!lessons.length) {
      list = `<div class="empty-state">
        <div class="empty-icon">${date.getDay() === 0 ? '🎉' : '☕️'}</div>
        <h3>${date.getDay() === 0 ? 'Воскресенье' : 'Пар нет'}</h3>
        <p>${date.getDay() === 0 ? 'Выходной — отдыхаем' : 'По текущей чётности недели занятий нет'}</p>
      </div>`;
    } else {
      let activeIndex = 0;
      lessons.forEach((entry, index) => {
        if (!entry.cancelled) activeIndex += 1;
        list += lessonCardHTML(entry, activeIndex, date, live, today);
        if (!entry.cancelled) {
          const next = lessons.slice(index + 1).find(l => !l.cancelled);
          const breakPeriod = breakAfter(entry, next);
          if (breakPeriod) {
            const current = date.getTime() === today.getTime() &&
              nowMinutes(new Date()) >= breakPeriod.start && nowMinutes(new Date()) < breakPeriod.end;
            list += `<div class="break-row${current ? ' is-current' : ''}">
              <span>${fmtTime(breakPeriod.start)}–${fmtTime(breakPeriod.end)}</span>
              <span class="line"></span>
              <span>${breakPeriod.big ? 'Большой перерыв' : 'Перерыв'}${current ? ' · ' + fmtCountdown((breakPeriod.end - nowMinutes(new Date())) * 60) : ''}</span>
            </div>`;
          }
        }
      });
    }
    return `
      ${weekNavHTML(monday)}
      ${dateStripHTML()}
      ${error}
      ${heroHTML(date, lessons, live, works)}
      <div class="section-heading">
        <h2>${date.getTime() === today.getTime() ? 'Расписание на сегодня' : 'Расписание дня'}</h2>
        <span class="count">${pluralPairs(active.length)}</span>
      </div>
      ${list}
      <div class="settings-foot">Обновлено ${formatUpdated(state.schedule.updated_at || state.lastFetch)} · ${semesterWeek(today)}-я неделя</div>`;
  }

  function nowMinutes(date) {
    return date.getHours() * 60 + date.getMinutes() + date.getSeconds() / 60;
  }

  function statusKey(live) {
    if (!live) return 'none';
    if (live.kind === 'lesson') return `lesson-${live.lesson.time}-${live.lesson.subject}-${live.phase}`;
    if (live.kind === 'break') return `break-${live.next?.time || ''}`;
    return live.kind;
  }

  function heroHTML(date, lessons, live, works) {
    const today = stripTime(new Date());
    const active = activeLessons(lessons);
    const tomorrow = addDays(date, 1);
    const tomorrowFirst = activeLessons(lessonsFor(tomorrow, parityFor(tomorrow)))[0];
    let label = '', title = '', meta = '', seconds = null, suffix = '', progress = null, start = '', end = '', isLive = false;

    if (date.getTime() !== today.getTime()) {
      const dayDiff = Math.round((date - today) / 86400000);
      label = dayDiff === 1 ? 'ЗАВТРА' : dayDiff === -1 ? 'ВЧЕРА' : DAY_NAMES[DAY_KEYS[date.getDay()]].toUpperCase();
      if (!active.length) {
        title = 'Пар нет';
        meta = date.getDay() === 0 ? 'Выходной' : 'Свободный день';
      } else {
        title = pluralPairs(active.length);
        meta = `с ${fmtTime(active[0].start)} до ${fmtTime(active[active.length - 1].end)}`;
        if (works.length) meta += ` · ${works.length} ${pluralWorks(works.length)}`;
      }
    } else if (live.kind === 'none') {
      label = 'СЕГОДНЯ';
      title = 'Пар нет';
      meta = tomorrowFirst ? `Завтра: ${tomorrowFirst.subject} в ${tomorrowFirst.time.split('-')[0]}` : 'Свободный день';
    } else if (live.kind === 'before') {
      label = 'ДО ПЕРВОЙ ПАРЫ';
      title = live.lesson.subject;
      meta = `${live.lesson.time} · каб. ${live.lesson.room}`;
      seconds = live.secondsLeft; suffix = 'до начала'; isLive = true;
      start = live.lesson.time.split('-')[0]; end = live.lesson.time.split('-')[1];
    } else if (live.kind === 'lesson') {
      const phase = live.phase === 'first' ? '1-я половина' : live.phase === 'mid' ? 'перерыв внутри пары' : '2-я половина';
      label = `ИДЁТ ПАРА · ${phase}`;
      title = live.lesson.subject;
      meta = `${live.lesson.time} · каб. ${live.lesson.room}${live.lesson.note ? ' · ' + live.lesson.note : ''}`;
      seconds = live.secondsLeft;
      suffix = live.phase === 'first' ? 'до перерыва' : live.phase === 'mid' ? 'до конца перерыва' : 'до конца пары';
      progress = live.progress; isLive = true;
      start = live.lesson.time.split('-')[0]; end = live.lesson.time.split('-')[1];
    } else if (live.kind === 'break') {
      label = live.big ? 'БОЛЬШОЙ ПЕРЕРЫВ' : 'ПЕРЕРЫВ';
      title = live.next ? `Далее: ${live.next.subject}` : 'Перерыв';
      meta = live.next ? `${live.next.time} · каб. ${live.next.room}` : 'Отдыхаем';
      seconds = live.secondsLeft; suffix = 'до начала'; progress = live.progress; isLive = true;
      end = fmtTime(live.endMinutes);
    } else {
      label = 'СЕГОДНЯ';
      title = 'Пары закончились';
      meta = `Было ${pluralPairs(live.totalToday || 0)}${tomorrowFirst ? ` · завтра ${tomorrowFirst.subject}` : ''}`;
    }

    const chips = works.slice(0, 2).map(w => `<span class="hero-chip">${icon('warning')}${esc(w.title || w.type)}</span>`).join('');
    return `<section class="hero">
      <div class="hero-top">${isLive ? '<span class="live-dot"></span>' : ''}<span class="hero-label">${esc(label)}</span></div>
      <h2 class="hero-title">${esc(title)}</h2>
      ${meta ? `<div class="hero-meta">${esc(meta)}</div>` : ''}
      ${seconds !== null ? `<div class="hero-timer-row"><span class="hero-countdown js-countdown">${fmtCountdown(seconds)}</span><span class="hero-suffix">${esc(suffix)}</span></div>` : ''}
      ${progress !== null ? `<div class="hero-progress"><div class="hero-progress-fill js-progress" style="width:${Math.round(progress * 100)}%"></div></div>` : ''}
      ${start || end ? `<div class="hero-times"><span>${esc(start)}</span><span>${esc(end)}</span></div>` : ''}
      ${chips ? `<div class="hero-chips">${chips}</div>` : ''}
    </section>`;
  }

  function lessonCardHTML(entry, index, date, live, today) {
    const isToday = date.getTime() === today.getTime();
    const isCurrent = isToday && live?.kind === 'lesson' &&
      live.lesson.time === entry.time && live.lesson.subject === entry.subject && !entry.cancelled;
    const isDone = isToday && !isCurrent && nowMinutes(new Date()) >= entry.end;
    const isCancelled = entry.cancelled;
    const accent = subjectColor(entry.subject);
    const badges = [];
    if (entry.change === 'replace') badges.push('<span class="badge is-change">Замена</span>');
    if (entry.change === 'extra') badges.push('<span class="badge is-change">Доп. пара</span>');
    if (entry.change === 'cancel') badges.push('<span class="badge is-cancel">Отменена</span>');
    if (entry.change === 'note') badges.push('<span class="badge is-note">Примечание</span>');
    if (entry.weeks && entry.weeks !== 'all') badges.push(`<span class="badge">${entry.weeks.join(',')} нед.</span>`);
    if (isCurrent) badges.push('<span class="badge is-current">Сейчас</span>');

    const works = entry.cancelled ? [] : worksFor(date, entry.subject, today);
    const teachers = entry.teachers?.length
      ? `<div class="pair-teacher">${icon('person')}<span>${esc(entry.teachers.join(', '))}</span></div>` : '';
    const note = entry.note
      ? `<div class="pair-note">${esc(entry.note)}</div>` : '';
    const workHTML = works.map(w => workBannerHTML(w, today)).join('');
    const progress = isCurrent && live?.kind === 'lesson'
      ? `<div class="pair-progress"><div class="pair-progress-fill js-progress" style="width:${Math.round(live.progress * 100)}%"></div></div>`
      : '';
    const classes = ['pair-card'];
    if (isCurrent) classes.push('is-current');
    if (isDone && !isCancelled) classes.push('is-done');
    if (isCancelled) classes.push('is-cancelled');
    return `<article class="${classes.join(' ')}" style="--subject-color:${accent}">
      <div class="pair-head">
        <div class="subject-glyph">${esc(abbr(entry.subject))}</div>
        <div class="pair-main">
          <div class="pair-badges"><span class="pair-index">Пара ${index}</span>${badges.join('')}</div>
          <h3 class="pair-title${isCancelled ? ' is-cancelled' : ''}">${esc(entry.subject)}</h3>
          ${teachers}
        </div>
      </div>
      <div class="pair-info">
        <span class="pair-chip">${icon('clock')}${esc(entry.time)}</span>
        <span class="pair-chip">${icon('pin')}каб. ${esc(entry.room || '—')}</span>
      </div>
      ${note}${workHTML}${progress}
    </article>`;
  }

  function workBannerHTML(work, today) {
    const days = Math.round((parseISO(work.date) - today) / 86400000);
    const when = days === 0 ? 'Сегодня' : days === 1 ? 'Завтра' : `Через ${days} ${pluralDays(days)}`;
    const cls = days <= 0 ? ' is-today' : '';
    return `<div class="work-banner${cls}">${icon('warning', 'icon')} ${esc(when)}: ${esc(work.title || work.type)}
      <span>${esc(work.type)}${work.note ? ' · ' + esc(work.note) : ''}</span></div>`;
  }

  function breakAfter(lesson, next) {
    if (!next || lesson.cancelled || next.cancelled) return null;
    const slot = SLOTS.find(s => s.start === lesson.start && s.end === lesson.end);
    if (!slot) return null;
    return BREAKS.find(b => b.start === slot.end && b.end === next.start) || null;
  }

  function renderWeek() {
    const monday = state.weekBase;
    const parity = parityFor(monday);
    const days = Array.from({ length: 7 }, (_, i) => addDays(monday, i));
    const today = stripTime(new Date());
    const works = worksBetween(monday, addDays(monday, 6))
      .filter(w => parseISO(w.date) >= today);
    const error = state.error
      ? `<div class="update-error">${icon('warning')}<span>${esc(state.error)}</span><button data-refresh>Повторить</button></div>`
      : '';
    const worksHTML = works.length ? `<div class="works-card">
      <h3>${icon('warning')} Работы на неделе</h3>
      ${works.map(w => {
        const daysLeft = Math.round((parseISO(w.date) - today) / 86400000);
        return `<div class="work-item">${icon('warning')}<div class="work-copy"><strong>${esc(w.title || w.type)}</strong><small>${esc(shortDate(parseISO(w.date)))} · ${esc(w.type)}</small></div>${daysLeft <= 1 ? `<span class="when">${daysLeft === 0 ? 'сегодня' : 'завтра'}</span>` : ''}</div>`;
      }).join('')}
    </div>` : '';
    const daysHTML = days.map((date, index) => {
      const lessons = lessonsFor(date, parity);
      const active = activeLessons(lessons);
      const isToday = date.getTime() === today.getTime();
      const open = state.expanded.has(iso(date));
      return `<section class="day-card${isToday ? ' is-today' : ''}${open ? ' is-open' : ''}">
        <button class="day-head" data-day-toggle="${iso(date)}">
          <span class="day-date-pill">${date.getDate()}</span>
          <span class="day-copy"><span class="day-title">${DAY_NAMES[DAY_KEYS[date.getDay()]]}${isToday ? '<span class="today-badge">сегодня</span>' : ''}</span>
          <span class="day-sub">${date.getDate()} ${MONTHS_GEN[date.getMonth()]}</span></span>
          <span class="day-count">${active.length ? pluralPairs(active.length) : 'нет пар'}</span>
          <span class="day-chevron">${icon('chevron-down')}</span>
        </button>
        <div class="day-body"><div class="day-body-inner"><div class="day-body-content">
          ${lessons.length ? lessons.map(l => {
            const accent = subjectColor(l.subject);
            const change = l.change ? `<span class="change-word">${l.change === 'replace' ? 'замена' : l.change === 'extra' ? 'доп.' : l.change === 'cancel' ? 'отмена' : 'прим.'}</span>` : '';
            return `<button class="week-lesson${l.cancelled ? ' is-cancelled' : ''}" data-open-day="${iso(date)}" style="--subject-color:${accent}">
              <span class="time"><b>${esc(l.time.split('-')[0])}</b>${esc(l.time.split('-')[1])}</span>
              <span class="bar"></span>
              <span class="copy"><span class="name">${esc(l.subject)}${change}</span><span class="details">каб. ${esc(l.room || '—')}${l.weeks && l.weeks !== 'all' ? ` · ${l.weeks.join(',')} нед.` : ''}</span></span>
            </button>`;
          }).join('') : `<div style="padding:14px 16px;color:var(--muted);font-size:12px">${date.getDay() === 0 ? 'Выходной — пар нет' : 'Пар нет'}</div>`}
        </div></div></div>
      </section>`;
    }).join('');
    return `${weekNavHTML(monday)}${error}${worksHTML}<div class="section-heading"><h2>Дни недели</h2></div>${daysHTML}
      <div class="settings-foot">Обновлено ${formatUpdated(state.schedule.updated_at || state.lastFetch)}</div>`;
  }

  function renderSettings() {
    const s = state.settings;
    const p = state.settings.parityOverride;
    const today = stripTime(new Date());
    const week = semesterWeek(today);
    const parity = parityFor(today);
    const repoUrl = 'https://github.com/MrFikus13/rt521-schedule';
    return `
      <div class="settings-title">Тема приложения</div>
      <div class="theme-options">
        <button class="theme-option${s.style === 'miuix' ? ' is-selected' : ''}" data-theme-style="miuix">
          <span class="theme-preview preview-miuix"></span>
          <span class="theme-copy"><span class="theme-name">MIUIX</span><span class="theme-desc">Официальная Miuix и Monet Material You</span></span>
          <span class="theme-check">${icon('check')}</span>
        </button>
        <button class="theme-option${s.style === 'liquid' ? ' is-selected' : ''}" data-theme-style="liquid">
          <span class="theme-preview preview-glass"></span>
          <span class="theme-copy"><span class="theme-name">iOS Liquid Glass</span><span class="theme-desc">Только в веб-версии: стекло, blur, глубина</span></span>
          <span class="theme-check">${icon('check')}</span>
        </button>
      </div>

      <div class="settings-title">Режим</div>
      <div class="segmented">
        <button data-mode="light" class="${s.mode === 'light' ? 'is-selected' : ''}">Светлая</button>
        <button data-mode="dark" class="${s.mode === 'dark' ? 'is-selected' : ''}">Тёмная</button>
        <button data-mode="auto" class="${s.mode === 'auto' ? 'is-selected' : ''}">Системная</button>
      </div>

      <div class="settings-title">Чётность недели</div>
      <div class="settings-group">
        <div class="settings-row">
          <span class="row-icon">${icon('calendar')}</span>
          <span class="row-copy"><span class="row-title">Определять автоматически</span><span class="row-sub">${week}-я неделя · ${parityLabel(parity)}${p ? ' (вручную)' : ''}</span></span>
          <button class="switch${p === 0 ? ' is-on' : ''}" data-parity-auto aria-label="Автоматическая чётность"></button>
        </div>
        ${p ? `<div class="segmented" style="margin:0 12px 12px"><button data-parity="1" class="${p === 1 ? 'is-selected' : ''}">Нечётная</button><button data-parity="2" class="${p === 2 ? 'is-selected' : ''}">Чётная</button></div>` : ''}
      </div>

      <div class="settings-title">Обновления</div>
      <div class="settings-group">
        <div class="settings-row">
          <span class="row-icon">${icon('clock')}</span>
          <span class="row-copy"><span class="row-title">Последнее обновление</span><span class="row-sub">${state.lastFetch ? formatUpdated(state.lastFetch) : '—'}</span></span>
        </div>
        <div class="settings-row is-button" data-refresh>
          <span class="row-icon">${icon('refresh')}</span>
          <span class="row-copy"><span class="row-title">Обновить сейчас</span><span class="row-sub">Скачать актуальный JSON с GitHub</span></span>
          <span class="chevron">${icon('chevron-right')}</span>
        </div>
        <a class="settings-row is-button" href="${repoUrl}/blob/main/data/schedule.json" target="_blank" rel="noopener" style="text-decoration:none;color:inherit">
          <span class="row-icon">${icon('book')}</span>
          <span class="row-copy"><span class="row-title">data/schedule.json</span><span class="row-sub">Расписание, замены и работы в GitHub</span></span>
          <span class="chevron">${icon('arrow-right')}</span>
        </a>
        <div class="settings-row">
          <span class="row-icon">${icon('calendar')}</span>
          <span class="row-copy"><span class="row-title">Автообновление</span><span class="row-sub">Ежедневно в 18:00 и 20:30, при открытии и по кнопке</span></span>
        </div>
      </div>

      <div class="settings-title">О приложении</div>
      <div class="settings-group">
        <div class="settings-row">
          <span class="row-icon">${icon('person')}</span>
          <span class="row-copy"><span class="row-title">Группа</span><span class="row-sub">${esc(state.schedule?.group || 'РТ-521')} · ${state.schedule?.semester || 5} семестр · ${esc(state.schedule?.academic_year || '')}</span></span>
        </div>
        <div class="settings-row">
          <span class="row-icon">${icon('book')}</span>
          <span class="row-copy"><span class="row-title">Версия</span><span class="row-sub">PWA 2.1 · Android APK 2.1</span></span>
        </div>
        <div class="settings-row">
          <span class="row-icon">${icon('arrow-right')}</span>
          <span class="row-copy"><a class="settings-link" href="${repoUrl}" target="_blank" rel="noopener">Исходный код на GitHub</a><span class="row-sub">data/schedule.json — расписание, замены, работы</span></span>
        </div>
      </div>
      <div class="settings-foot">РТ-521 · ${esc(state.schedule?.academic_year || '2026/2027')}<br>MIUIX × iOS Liquid Glass</div>`;
  }

  function formatUpdated(value) {
    if (!value) return '—';
    const date = typeof value === 'number' ? new Date(value) : new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    try {
      return new Intl.DateTimeFormat('ru-RU', {
        day: 'numeric', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      }).format(date);
    } catch {
      return date.toLocaleString('ru-RU');
    }
  }

  /* ============================================================
     Interaction
     ============================================================ */

  function setView(view) {
    state.view = view;
    if (view === 'week') state.weekBase = mondayOf(state.selectedDate);
    render();
    window.scrollTo(0, 0);
  }

  function showToast(message) {
    const toast = $('#toast');
    toast.textContent = message;
    toast.classList.add('is-visible');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove('is-visible'), 2800);
  }

  document.addEventListener('click', (event) => {
    const target = event.target;
    const themeStyle = target.closest('[data-theme-style]');
    if (themeStyle) {
      const style = themeStyle.dataset.themeStyle;
      state.settings.style = style;
      saveSettings();
      applyTheme();
      render();
      showToast(style === 'liquid' ? 'iOS Liquid Glass включён' : 'MIUIX включён');
      return;
    }
    const mode = target.closest('button[data-mode]');
    if (mode) {
      state.settings.mode = mode.dataset.mode;
      saveSettings();
      applyTheme();
      render();
      return;
    }
    const date = target.closest('[data-date]');
    if (date) {
      state.selectedDate = parseISO(date.dataset.date);
      state.expanded = new Set();
      render();
      return;
    }
    const prev = target.closest('[data-prev-week]');
    if (prev) {
      if (state.view === 'week') state.weekBase = addDays(state.weekBase, -7);
      else state.selectedDate = addDays(state.selectedDate, -7);
      render();
      return;
    }
    const next = target.closest('[data-next-week]');
    if (next) {
      if (state.view === 'week') state.weekBase = addDays(state.weekBase, 7);
      else state.selectedDate = addDays(state.selectedDate, 7);
      render();
      return;
    }
    const today = target.closest('[data-today]');
    if (today) {
      state.selectedDate = stripTime(new Date());
      state.weekBase = mondayOf(state.selectedDate);
      render();
      return;
    }
    const toggle = target.closest('[data-day-toggle]');
    if (toggle) {
      const key = toggle.dataset.dayToggle;
      if (state.expanded.has(key)) state.expanded.delete(key);
      else state.expanded.add(key);
      render();
      return;
    }
    const openDay = target.closest('[data-open-day]');
    if (openDay) {
      state.selectedDate = parseISO(openDay.dataset.openDay);
      state.view = 'day';
      render();
      window.scrollTo(0, 0);
      return;
    }
    const refresh = target.closest('[data-refresh]');
    if (refresh) {
      fetchSchedule(true);
      return;
    }
    const parityAuto = target.closest('[data-parity-auto]');
    if (parityAuto) {
      state.settings.parityOverride = state.settings.parityOverride ? 0 : (parityFor(stripTime(new Date())) === 'odd' ? 1 : 2);
      saveSettings();
      render();
      return;
    }
    const parity = target.closest('[data-parity]');
    if (parity) {
      state.settings.parityOverride = Number(parity.dataset.parity);
      saveSettings();
      render();
      return;
    }
    const nav = target.closest('.nav-item');
    if (nav) {
      setView(nav.dataset.view);
    }
  });

  $('#refreshBtn').addEventListener('click', () => fetchSchedule(true));
  $('#themeBtn').addEventListener('click', () => setView('settings'));

  /* ============================================================
     Loading / refresh
     ============================================================ */

  async function fetchSchedule(manual = false) {
    if (state.refreshing) return;
    state.refreshing = true;
    $('#refreshBtn')?.classList.add('is-refreshing');
    try {
      const url = new URL('data/schedule.json', document.baseURI);
      url.searchParams.set('ts', Date.now());
      const response = await fetch(url, { cache: 'no-store' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const text = await response.text();
      const parsed = JSON.parse(text);
      if (!parsed.schedule) throw new Error('В JSON нет schedule');
      state.schedule = parsed;
      state.error = '';
      state.lastFetch = Date.now();
      writeStorage(`${STORAGE}_schedule`, text);
      writeStorage(`${STORAGE}_last_fetch`, String(state.lastFetch));
      render();
      if (manual) showToast('Расписание обновлено');
    } catch (error) {
      state.error = `Не удалось обновить: ${error.message || 'сеть недоступна'}`;
      render();
      if (manual) showToast('Не удалось обновить расписание');
    } finally {
      state.refreshing = false;
      $('#refreshBtn')?.classList.remove('is-refreshing');
    }
  }

  function loadCached() {
    const raw = readStorage(`${STORAGE}_schedule`);
    if (!raw) return false;
    try {
      const parsed = JSON.parse(raw);
      if (!parsed.schedule) return false;
      state.schedule = parsed;
      state.lastFetch = Number(readStorage(`${STORAGE}_last_fetch`) || 0);
      return true;
    } catch {
      return false;
    }
  }

  /* ============================================================
     Clock / auto refresh
     ============================================================ */

  function refreshBoundary(now = new Date()) {
    const day = stripTime(now);
    const at18 = new Date(day.getFullYear(), day.getMonth(), day.getDate(), 18, 0, 0);
    const at2030 = new Date(day.getFullYear(), day.getMonth(), day.getDate(), 20, 30, 0);
    if (now >= at2030) return { latest: at2030, next: addDays(day, 1).getTime() + 18 * 3600000 };
    if (now >= at18) return { latest: at18, next: at2030.getTime() };
    const yesterday = addDays(day, -1);
    return {
      latest: new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate(), 20, 30, 0),
      next: at18.getTime(),
    };
  }

  function checkAutoRefresh() {
    if (!state.schedule) return;
    const now = new Date();
    const boundary = refreshBoundary(now);
    if (state.lastFetch < boundary.latest.getTime()) {
      fetchSchedule(false);
    }
  }

  function tick() {
    const now = new Date();
    if (state.view === 'day' && state.selectedDate.getTime() === stripTime(now).getTime() && state.schedule) {
      const lessons = lessonsFor(state.selectedDate);
      const live = liveStatus(now, lessons);
      const key = statusKey(live);
      if (key !== state.lastStatusKey) {
        state.lastStatusKey = key;
        render();
      } else if (live.kind === 'lesson' || live.kind === 'before' || live.kind === 'break') {
        $$('.js-countdown').forEach(el => { el.textContent = fmtCountdown(live.secondsLeft); });
        $$('.js-progress').forEach(el => {
          if (live.progress !== undefined) el.style.width = `${Math.round(live.progress * 100)}%`;
        });
      }
    }
    if (Math.floor(now.getTime() / 30000) !== tick.lastAuto) {
      tick.lastAuto = Math.floor(now.getTime() / 30000);
      checkAutoRefresh();
    }
  }

  /* ============================================================
     PWA / install
     ============================================================ */

  let installPrompt = null;
  const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone;

  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    installPrompt = event;
    if (!state.settings.installDismissed) showInstallBanner('Установить приложение', 'Быстрый доступ к расписанию');
  });

  function showInstallBanner(title, text) {
    const banner = $('#installBanner');
    if (!banner || isStandalone || state.settings.installDismissed) return;
    banner.querySelector('strong').textContent = title;
    banner.querySelector('.install-banner-copy span').textContent = text;
    banner.hidden = false;
  }

  function initInstall() {
    if (isIOS && !isStandalone && !state.settings.installDismissed) {
      showInstallBanner('Установить на iPhone', 'Поделиться → «На экран Домой»');
    }
    $('#installBanner')?.addEventListener('click', async (event) => {
      if (event.target.closest('#installClose')) {
        state.settings.installDismissed = true;
        saveSettings();
        $('#installBanner').hidden = true;
        return;
      }
      if (installPrompt) {
        installPrompt.prompt();
        await installPrompt.userChoice;
        installPrompt = null;
        $('#installBanner').hidden = true;
      }
    });
  }

  if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => {});
    });
  }

  /* ============================================================
     Init
     ============================================================ */

  function init() {
    const params = new URLSearchParams(location.search);
    if (params.get('date') && /^\d{4}-\d{2}-\d{2}$/.test(params.get('date'))) {
      state.selectedDate = parseISO(params.get('date'));
      state.weekBase = mondayOf(state.selectedDate);
    }
    if (params.get('theme') === 'liquid' || params.get('theme') === 'miuix') {
      state.settings.style = params.get('theme');
      saveSettings();
    }
    if (['light', 'dark', 'auto'].includes(params.get('mode'))) {
      state.settings.mode = params.get('mode');
      saveSettings();
    }
    if (params.get('view') === 'week') state.view = 'week';
    if (params.get('install') === '0') {
      state.settings.installDismissed = true;
      saveSettings();
    }
    applyTheme();
    loadCached();
    if (state.schedule) {
      render();
      fetchSchedule(false);
    } else {
      render();
      fetchSchedule(false);
    }
    initInstall();
    setInterval(tick, 1000);
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) checkAutoRefresh();
    });
    window.addEventListener('focus', checkAutoRefresh);
    window.addEventListener('online', () => {
      if (state.error) fetchSchedule(false);
    });
  }

  init();
})();
