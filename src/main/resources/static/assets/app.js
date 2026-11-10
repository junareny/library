(() => {
  'use strict';

  const i18n = window.LibraryI18n;
  const t = i18n.t;
  const $ = (selector) => document.querySelector(selector);
  const SESSION_KEY = 'yuejian-session';
  const PAGE_SIZE = 8;
  const palettes = [
    ['#365447', '#e4dfc7', '#e6eae0'],
    ['#dac5a1', '#444c36', '#eeeadf'],
    ['#c6d1c0', '#384b3a', '#e8ede2'],
    ['#46596a', '#e7e1cd', '#e8ebeb'],
    ['#b66c51', '#f5e4c6', '#f0e8dd'],
    ['#d9cfb2', '#4d5942', '#eeeee3'],
    ['#64775b', '#efe8cd', '#e9ecdf'],
    ['#49665c', '#e9e3c6', '#e5eae0'],
    ['#cab98c', '#434d3a', '#edeadf'],
    ['#7e8d7b', '#f0ead1', '#e7ece3'],
  ];
  const state = {
    books: [],
    bookCache: new Map(),
    loans: [],
    totalBooks: null,
    view: location.hash === '#loans' ? 'loans' : 'catalog',
    filter: 'all',
    page: 0,
    query: '',
    loading: false,
    loadError: '',
    authMode: 'login',
    session: readSession(),
    pending: new Set(),
    detailId: null,
    returnLoan: null,
    searchSequence: 0,
    authBusy: false,
    sessionGeneration: 0,
  };
  const errorTexts = {
    USERNAME_EXISTS: '这个用户名已被使用，请换一个试试。',
    OUT_OF_STOCK: '这本书暂时没有可借副本，可以看看其他书。',
    ALREADY_BORROWED: '你已经借阅了这本书，可在“我的借阅”中查看。',
    LOAN_LIMIT_EXCEEDED: '最多可同时借阅 5 本书，请先归还一本。',
    ALREADY_RETURNED: '这本书已经归还，借阅列表已更新。',
    RESOURCE_NOT_FOUND: '没有找到这条记录，请刷新后再试。',
    VALIDATION_ERROR: '请检查填写内容。用户名为 3–50 个字符，密码至少 8 个字符。',
    SERVICE_UNAVAILABLE: '服务暂时不可用，请稍后重试。',
    INTERNAL_ERROR: '操作暂时未完成，请稍后重试。',
  };

  function readSession() {
    try {
      const session = JSON.parse(sessionStorage.getItem(SESSION_KEY));
      if (
        session &&
        typeof session.token === 'string' &&
        typeof session.username === 'string' &&
        session.expiresAt > Date.now()
      )
        return session;
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* 浏览器可能禁用会话存储，此时仍可在内存中维持登录状态。 */
    }
    return null;
  }
  function saveSession() {
    try {
      if (state.session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(state.session));
      else sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* 会话存储不可用时，将当前登录状态保留在内存中。 */
    }
  }
  const escape = (value) =>
    String(value ?? '').replace(
      /[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c],
    );
  const icon = (name) => `<svg class="icon" aria-hidden="true"><use href="#i-${name}"/></svg>`;
  const bookById = (id) => state.bookCache.get(Number(id));
  const loanFor = (id) => state.loans.find((loan) => loan.bookId === Number(id));
  function notice(message, isError = false) {
    const node = $('#notice');
    node.classList.toggle('error', isError);
    node.innerHTML = `<span>${escape(message)}</span><button type="button" data-action="dismiss-notice" aria-label="${t('关闭提示')}">${t('关闭')}</button>`;
    node.hidden = false;
  }
  function clearSession() {
    state.session = null;
    state.sessionGeneration++;
    state.loans = [];
    saveSession();
    renderAccount();
    renderStats();
    renderShelf();
  }
  async function api(path, options = {}) {
    const headers = {
      Accept: 'application/json',
      ...options.headers,
      'Accept-Language': i18n.language,
    };
    const requestLanguage = i18n.language;
    if (options.body) headers['Content-Type'] = 'application/json';
    const usedSession = state.session;
    if (usedSession) headers.Authorization = `Bearer ${usedSession.token}`;
    let response;
    try {
      response = await fetch(path, { ...options, headers });
    } catch (error) {
      if (error.name === 'AbortError') throw error;
      const failure = new Error(t('无法连接服务。操作结果可能尚未确认，请刷新借阅列表后再试。'));
      failure.code = 'NETWORK_ERROR';
      throw failure;
    }
    const data = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok || data?.code !== '0') {
      let message =
        (requestLanguage === i18n.language ? data?.msg : null) ||
        t(errorTexts[data?.code] || '请求未完成，请稍后重试。') ||
        t('请求未完成，请稍后重试。');
      if (response.status === 401) {
        if (usedSession && usedSession === state.session) {
          clearSession();
          message = t('登录已过期，请重新登录后继续。');
          notice(message, true);
        } else message = t('用户名或密码不正确，请检查后重试。');
      }
      const error = new Error(message);
      error.code = data?.code || 'REQUEST_FAILED';
      error.status = response.status;
      throw error;
    }
    return data.body;
  }
  function cover(book) {
    const [background, foreground] = palettes[(book.id - 1) % palettes.length];
    const titleSize = book.title.length > 30 ? 14 : book.title.length > 24 ? 16 : 18;
    return `<div class="book-cover theme-${book.id % 5}" style="--cover-bg:${background};--cover-fg:${foreground}" aria-hidden="true"><span class="cover-kicker">YUEJIAN · COLLECTION</span><span class="cover-title" style="font-size:${titleSize}px">${escape(book.title)}</span><span class="cover-art"></span><span class="cover-author">${escape(book.author)}</span></div>`;
  }
  function renderAccount() {
    $('#account-area').innerHTML = state.session
      ? `<div class="account"><span class="avatar" aria-hidden="true">${escape([...state.session.username][0].toUpperCase())}</span><span class="account-name" title="${escape(state.session.username)}">${escape(state.session.username)}</span><button class="text-button" data-action="logout">${t('退出')}</button></div>`
      : `<button class="button button-light button-small" data-action="login">${icon('user')}${t('登录 / 注册')}</button>`;
  }
  function renderStats() {
    $('#stat-books').innerHTML = `${state.totalBooks ?? '—'}<small>${t('本')}</small>`;
    $('#stat-loans').innerHTML =
      `${state.session ? state.loans.length : '—'}<small>${t('/ 5 本')}</small>`;
    $('#nav-loan-count').textContent = state.session ? state.loans.length : '0';
    const overdue = state.loans.filter((loan) => loan.status === 'OVERDUE').length;
    $('#stat-loans-note').textContent = !state.session
      ? t('登录后开启阅读之旅')
      : overdue
        ? t('逾期数量', { count: overdue })
        : t('借阅余量', { count: 5 - state.loans.length });
  }
  function setView() {
    state.view = location.hash === '#loans' ? 'loans' : 'catalog';
    state.page = 0;
    $('#catalog-hero').hidden = state.view === 'loans';
    $('#search-form').hidden = state.view === 'loans';
    $('#catalog-filters').hidden = state.view === 'loans';
    $('#breadcrumb-current').textContent = state.view === 'loans' ? t('我的借阅') : t('探索书库');
    $('#section-eyebrow').textContent =
      state.view === 'loans' ? 'YOUR READING JOURNEY' : 'THE COLLECTION';
    $('#shelf-label').textContent = state.view === 'loans' ? t('我的借阅') : t('发现好书');
    $('#sort-label').textContent =
      state.view === 'loans' ? t('按借阅时间倒序 · 最多同时借阅 5 本') : t('按馆藏顺序排列');
    document.querySelectorAll('[data-nav]').forEach((a) => {
      const active = a.dataset.nav === state.view;
      a.classList.toggle('active', active);
      if (active) a.setAttribute('aria-current', 'page');
      else a.removeAttribute('aria-current');
    });
    renderShelf();
  }
  function emptyState(title, description, action = '') {
    return `<div class="empty-state">${icon('book')}<h3>${escape(title)}</h3><p>${escape(description)}</p>${action}</div>`;
  }
  function renderShelf() {
    const grid = $('#book-grid');
    grid.classList.toggle('loan-grid', state.view === 'loans');
    grid.setAttribute('aria-busy', String(state.loading));
    $('#pagination').innerHTML = '';
    $('#result-count').textContent = '';
    if (state.view === 'loans' && !state.session) {
      grid.innerHTML = emptyState(
        t('你的阅读旅程，从这里开始'),
        t('登录后，即可查看借阅中的图书与归还日期。'),
        `<button class="button button-primary" data-action="login">${t('登录 / 注册')}</button>`,
      );
      return;
    }
    if (state.loading) {
      grid.innerHTML = Array.from(
        { length: state.view === 'loans' ? 2 : 4 },
        () => `<div class="skeleton" aria-label="${t('正在加载')}"></div>`,
      ).join('');
      return;
    }
    if (state.loadError) {
      grid.innerHTML = emptyState(
        t('暂时无法加载'),
        state.loadError,
        `<button class="button button-light" data-action="refresh">${t('重新加载')}</button>`,
      );
      return;
    }
    if (state.view === 'loans') {
      $('#result-count').textContent = t('借阅数量', { count: state.loans.length });
      grid.innerHTML = state.loans.length
        ? state.loans.map(loanCard).join('')
        : emptyState(
            t('书架正等着你的下一本书'),
            t('去书库逛逛，把好奇的世界带回来。'),
            `<a class="button button-primary" href="#catalog">${t('探索书库')}</a>`,
          );
      return;
    }
    if (state.filter === 'borrowed' && !state.session) {
      grid.innerHTML = emptyState(
        t('登录，找到你正在读的书'),
        t('登录后可筛选已借阅书籍，或管理你的借阅。'),
        `<button class="button button-primary" data-action="login">${t('登录 / 注册')}</button>`,
      );
      return;
    }
    const filtered = state.books.filter((book) =>
      state.filter === 'available'
        ? book.availableCopies > 0
        : state.filter === 'borrowed'
          ? loanFor(book.id)
          : true,
    );
    $('#result-count').textContent = t('结果数量', {
      count: filtered.length,
      kind: t(state.query ? '搜索结果' : '书籍'),
    });
    const pages = Math.ceil(filtered.length / PAGE_SIZE);
    state.page = Math.max(0, Math.min(state.page, pages - 1));
    grid.innerHTML = filtered.length
      ? filtered
          .slice(state.page * PAGE_SIZE, (state.page + 1) * PAGE_SIZE)
          .map(bookCard)
          .join('')
      : emptyState(
          state.query ? t('还没找到这本书') : t('这里暂时没有书籍'),
          state.query
            ? t('试试其他书名关键词，或清除搜索查看全部馆藏。')
            : state.filter === 'borrowed'
              ? t('挑一本感兴趣的书，开启下一段阅读。')
              : t('换一个筛选条件，看看其他好书。'),
          `<button class="button button-light" data-action="reset-search">${t('查看全部书籍')}</button>`,
        );
    if (filtered.length)
      $('#pagination').innerHTML =
        `<span>${t('分页范围', { start: state.page * PAGE_SIZE + 1, end: Math.min((state.page + 1) * PAGE_SIZE, filtered.length), total: filtered.length })}</span><div class="page-buttons"><button data-page="${state.page - 1}" aria-label="${t('上一页')}" ${state.page === 0 ? 'disabled' : ''}>‹</button>${Array.from({ length: pages }, (_, page) => `<button data-page="${page}" class="${page === state.page ? 'active' : ''}" ${page === state.page ? 'aria-current="page"' : ''}>${page + 1}</button>`).join('')}<button data-page="${state.page + 1}" aria-label="${t('下一页')}" ${state.page + 1 >= pages ? 'disabled' : ''}>›</button></div>`;
  }
  function bookCard(book) {
    const borrowed = loanFor(book.id);
    const pending = state.pending.has(book.id);
    const [, , stage] = palettes[(book.id - 1) % palettes.length];
    return `<article class="book-card"><button class="cover-button" data-detail="${book.id}" aria-label="${escape(t('查看详情标签', { title: book.title }))}"><span class="cover-stage" style="background:${stage}"></span>${cover(book)}</button><div class="book-info"><h3 class="book-title"><button data-detail="${book.id}" title="${escape(book.title)}">${escape(book.title)}</button></h3><p class="book-author" title="${escape(book.author)}">${escape(book.author)}</p><div class="book-bottom-row"><span class="availability ${borrowed ? 'borrowed' : book.availableCopies ? '' : 'unavailable'}">${borrowed ? t('正在借阅') : book.availableCopies ? t('可借数量', { count: book.availableCopies }) : t('暂时借罄')}</span><button class="borrow-button" data-borrow="${book.id}" aria-label="${escape(t('借阅按钮标签', { action: t(borrowed ? '已借阅' : '借阅'), title: book.title }))}" ${borrowed || pending || !book.availableCopies ? 'disabled' : ''}>${pending ? t('借阅中…') : borrowed ? t('已借阅') : !book.availableCopies ? t('暂无库存') : t('借阅')}${!borrowed && book.availableCopies && !pending ? ' ↗' : ''}</button></div></div></article>`;
  }
  function formatDate(value) {
    return new Intl.DateTimeFormat(i18n.language, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date(value));
  }
  function loanCard(loan) {
    const book = bookById(loan.bookId) || {
      id: loan.bookId,
      title: t('书目编号', { id: loan.bookId }),
      author: '',
    };
    const overdue = loan.status === 'OVERDUE';
    const remaining = Math.max(0, Math.ceil((new Date(loan.dueAt) - Date.now()) / 86400000));
    return `<article class="loan-card">${cover(book)}<div class="loan-copy"><h3>${escape(book.title)}</h3><p>${escape(book.author)}</p><span class="loan-status ${overdue ? 'overdue' : ''}">${overdue ? t('已逾期 · 请及时归还') : t('借阅中')}</span><div class="loan-dates">${t('借阅日期')} <strong>${formatDate(loan.borrowedAt)}</strong><br>${t('应还日期')} <strong>${formatDate(loan.dueAt)}</strong></div><div class="loan-action-row"><button class="button button-light" data-return="${loan.id}">${t('归还图书')}</button><small>${overdue ? t('逾期仍占用借阅额度') : t('剩余天数', { count: remaining })}</small></div></div></article>`;
  }
  async function fetchBooks() {
    const sequence = ++state.searchSequence;
    const result = await api('/IBookBiz/queryBook/page', {
      method: 'POST',
      body: JSON.stringify({ pageCurrent: 1, pageSize: 100, dto: { title: state.query } }),
    });
    if (sequence !== state.searchSequence) return;
    state.books = result.data;
    result.data.forEach((book) => state.bookCache.set(book.id, book));
    if (!state.query) state.totalBooks = result.total;
  }
  async function fetchLoans() {
    const generation = state.sessionGeneration;
    if (!state.session) {
      state.loans = [];
      return;
    }
    const result = await api('/ILoanBiz/queryCurrentLoan/page', {
      method: 'POST',
      body: JSON.stringify({ pageCurrent: 1, pageSize: 100 }),
    });
    if (generation !== state.sessionGeneration) return;
    const missing = result.data.filter((loan) => !bookById(loan.bookId));
    await Promise.all(
      missing.map(async (loan) => {
        const book = await api('/IBookBiz/queryBook', {
          method: 'POST',
          body: JSON.stringify({ id: loan.bookId }),
        });
        state.bookCache.set(book.id, book);
      }),
    );
    if (generation === state.sessionGeneration) state.loans = result.data;
  }
  let refreshSequence = 0;
  async function refresh() {
    const sequence = ++refreshSequence;
    state.loading = true;
    state.loadError = '';
    renderShelf();
    try {
      await Promise.all([fetchBooks(), fetchLoans()]);
    } catch (error) {
      if (sequence === refreshSequence) state.loadError = error.message;
    } finally {
      if (sequence === refreshSequence) {
        state.loading = false;
        renderStats();
        renderShelf();
      }
    }
  }
  function openAuth(mode = 'login') {
    if (state.authBusy) return;
    state.authMode = mode;
    $('#auth-error').hidden = true;
    $('#auth-title').textContent = mode === 'login' ? t('欢迎回到阅间') : t('开启你的阅读旅程');
    $('#auth-description').textContent =
      mode === 'login' ? t('登录，继续你的阅读旅程。') : t('创建一个账号，让好书与你相遇。');
    $('#auth-submit').textContent = mode === 'login' ? t('登录') : t('注册并登录');
    $('#auth-switch-copy').innerHTML =
      `${mode === 'login' ? t('还没有账号？') : t('已经有账号？')}<button type="button" id="auth-switch">${mode === 'login' ? t('立即注册') : t('返回登录')}</button>`;
    $('#password').autocomplete = mode === 'login' ? 'current-password' : 'new-password';
    if (!$('#auth-dialog').open) {
      $('#auth-form').reset();
      $('#password').type = 'password';
      $('#toggle-password').textContent = t('显示');
      $('#auth-dialog').showModal();
    }
    $('#username').focus();
  }
  async function submitAuth(event) {
    event.preventDefault();
    if (state.authBusy) return;
    const username = $('#username').value;
    const password = $('#password').value;
    const errorNode = $('#auth-error');
    errorNode.hidden = true;
    let errorMessage = '';
    if (!/^\S{3,50}$/u.test(username))
      errorMessage = t('用户名需要 3–50 个字符，且不能包含空白字符。');
    else if (password.length < 8 || new TextEncoder().encode(password).length > 72)
      errorMessage = t('密码至少 8 个字符，且最多 72 个 UTF-8 字节。');
    if (errorMessage) {
      errorNode.textContent = errorMessage;
      errorNode.hidden = false;
      return;
    }
    state.authBusy = true;
    $('#language-select').disabled = true;
    $('#auth-submit').disabled = true;
    $('#auth-submit').textContent = t('请稍候…');
    const registering = state.authMode === 'register';
    let registered = false;
    try {
      const body = JSON.stringify({ username, password });
      if (registering) {
        await api('/IAuthBiz/register', { method: 'POST', body });
        registered = true;
      }
      const result = await api('/IAuthBiz/login', { method: 'POST', body });
      state.sessionGeneration++;
      state.session = {
        username,
        token: result.accessToken,
        expiresAt: Date.now() + result.expiresIn * 1000,
      };
      saveSession();
      renderAccount();
      $('#auth-dialog').close();
      $('#auth-form').reset();
      notice(
        registering
          ? t('注册成功，欢迎来到阅间。挑一本书，开启阅读吧。')
          : t('欢迎用户', { name: username }),
      );
      await refresh();
    } catch (error) {
      errorNode.textContent = registered
        ? t('自动登录失败', { message: error.message })
        : error.message;
      errorNode.hidden = false;
    } finally {
      state.authBusy = false;
      $('#language-select').disabled = false;
      $('#auth-submit').disabled = false;
      $('#auth-submit').textContent = state.authMode === 'login' ? t('登录') : t('注册并登录');
    }
  }
  function detail(id) {
    const book = bookById(id);
    if (!book) return;
    state.detailId = book.id;
    const borrowed = loanFor(id);
    $('#detail-content').innerHTML =
      `<div class="detail-layout"><div class="detail-cover">${cover(book)}</div><div><p class="section-eyebrow">YUEJIAN COLLECTION / ${String(book.id).padStart(2, '0')}</p><h2 id="detail-title">${escape(book.title)}</h2><p class="detail-author">${escape(book.author)}</p><dl class="detail-meta"><dt>ISBN</dt><dd>${escape(book.isbn || t('暂无'))}</dd><dt>${t('馆藏数量')}</dt><dd>${book.totalCopies} ${t('本')}</dd><dt>${t('当前可借')}</dt><dd>${book.availableCopies} ${t('本')}</dd></dl></div></div><p class="detail-policy">${t('每次借阅 30 天，最多可同时借阅 5 本。请按时归还，让好书继续流转。')}</p><div class="detail-actions"><button class="button button-primary" data-borrow="${book.id}" ${borrowed || !book.availableCopies || state.pending.has(book.id) ? 'disabled' : ''}>${borrowed ? t('已在你的借阅中') : book.availableCopies ? t('借阅这本书') : t('暂无可借副本')}</button></div>`;
    if (!$('#detail-dialog').open) $('#detail-dialog').showModal();
  }
  async function borrow(id) {
    if (!state.session) {
      $('#detail-dialog').close();
      openAuth();
      return;
    }
    if (state.pending.has(id)) return;
    state.pending.add(id);
    renderShelf();
    if ($('#detail-dialog').open) detail(id);
    try {
      const loan = await api('/ILoanBiz/borrowBook', {
        method: 'POST',
        body: JSON.stringify({ bookId: id }),
      });
      $('#detail-dialog').close();
      notice(
        t('借阅成功提示', {
          title: bookById(id)?.title || t('图书'),
          date: formatDate(loan.dueAt),
        }),
      );
      await refresh();
    } catch (error) {
      $('#detail-dialog').close();
      await refresh();
      notice(error.message, true);
    } finally {
      state.pending.delete(id);
      renderShelf();
    }
  }
  function openReturn(id) {
    const loan = state.loans.find((item) => item.id === id);
    if (!loan) return;
    state.returnLoan = loan;
    $('#return-description').textContent = t('归还确认提示', {
      title: bookById(loan.bookId)?.title || t('图书'),
    });
    $('#return-error').hidden = true;
    $('#confirm-return').disabled = false;
    $('#confirm-return').textContent = t('确认归还');
    $('#return-dialog').showModal();
  }
  async function confirmReturn() {
    const loan = state.returnLoan;
    if (!loan || $('#confirm-return').disabled) return;
    $('#confirm-return').disabled = true;
    $('#language-select').disabled = true;
    $('#confirm-return').textContent = t('归还中…');
    try {
      await api('/ILoanBiz/returnBook', { method: 'POST', body: JSON.stringify({ id: loan.id }) });
      state.returnLoan = null;
      $('#return-dialog').close();
      notice(t('归还成功。谢谢你，让好书继续被阅读。'));
      await refresh();
    } catch (error) {
      await refresh();
      if (error.code === 'ALREADY_RETURNED') {
        $('#return-dialog').close();
        notice(error.message);
      } else {
        $('#return-error').textContent = error.message;
        $('#return-error').hidden = false;
      }
    } finally {
      $('#confirm-return').disabled = false;
      $('#language-select').disabled = false;
      $('#confirm-return').textContent = t('确认归还');
    }
  }

  document.addEventListener('click', (event) => {
    const button = event.target.closest('button');
    if (!button || button.disabled) return;
    if (button.dataset.close) {
      if (button.dataset.close === 'auth-dialog' && state.authBusy) return;
      if (button.dataset.close === 'return-dialog' && $('#confirm-return').disabled) return;
      $(`#${button.dataset.close}`).close();
    } else if (button.id === 'auth-switch')
      openAuth(state.authMode === 'login' ? 'register' : 'login');
    else if (button.id === 'toggle-password') {
      const reveal = $('#password').type === 'password';
      $('#password').type = reveal ? 'text' : 'password';
      button.textContent = reveal ? t('隐藏') : t('显示');
      button.setAttribute('aria-label', reveal ? t('隐藏密码') : t('显示密码'));
    } else if (button.dataset.action === 'login') openAuth();
    else if (button.dataset.action === 'logout') {
      clearSession();
      notice(t('已退出登录，仍可继续浏览书库。'));
    } else if (button.dataset.action === 'dismiss-notice') $('#notice').hidden = true;
    else if (button.dataset.action === 'refresh') refresh();
    else if (button.dataset.action === 'reset-search') {
      state.query = '';
      state.filter = 'all';
      state.page = 0;
      $('#search-input').value = '';
      updateFilters();
      refresh();
    } else if (button.dataset.filter) {
      state.filter = button.dataset.filter;
      state.page = 0;
      updateFilters();
      renderShelf();
    } else if (button.dataset.page !== undefined) {
      state.page = Number(button.dataset.page);
      renderShelf();
      $('#shelf').scrollIntoView({ behavior: 'smooth' });
    } else if (button.dataset.detail) detail(Number(button.dataset.detail));
    else if (button.dataset.borrow) borrow(Number(button.dataset.borrow));
    else if (button.dataset.return) openReturn(Number(button.dataset.return));
  });
  function updateFilters() {
    document.querySelectorAll('[data-filter]').forEach((button) => {
      const active = state.filter === button.dataset.filter;
      button.classList.toggle('active', active);
      button.setAttribute('aria-pressed', String(active));
    });
  }
  $('#language-select').addEventListener('change', () => {
    i18n.setLanguage($('#language-select').value);
    state.loadError = '';
    $('#notice').hidden = true;
    $('#auth-error').hidden = true;
    $('#return-error').hidden = true;
    renderAccount();
    renderStats();
    setView();
    if ($('#auth-dialog').open) openAuth(state.authMode);
    if ($('#detail-dialog').open) detail(state.detailId);
    if ($('#return-dialog').open && state.returnLoan) openReturn(state.returnLoan.id);
    refresh();
  });
  $('#auth-form').addEventListener('submit', submitAuth);
  $('#confirm-return').addEventListener('click', confirmReturn);
  $('#auth-dialog').addEventListener('cancel', (event) => {
    if (state.authBusy) event.preventDefault();
  });
  $('#return-dialog').addEventListener('cancel', (event) => {
    if ($('#confirm-return').disabled) event.preventDefault();
  });
  $('#auth-dialog').addEventListener('close', () => {
    $('#auth-form').reset();
  });
  $('#search-form').addEventListener('submit', (event) => {
    event.preventDefault();
    state.query = $('#search-input').value.trim();
    state.page = 0;
    refresh();
  });
  $('#search-input').addEventListener('input', () => {
    if (!$('#search-input').value && state.query) {
      state.query = '';
      state.page = 0;
      refresh();
    }
  });
  window.addEventListener('hashchange', () => {
    setView();
    if (state.view === 'loans' && state.session) refresh();
  });
  window.addEventListener('focus', () => {
    if (state.session) refresh();
  });
  setInterval(() => {
    if (state.session && Date.now() >= state.session.expiresAt) {
      clearSession();
      notice(t('登录已过期，请重新登录后继续。'), true);
    }
  }, 30000);
  renderAccount();
  renderStats();
  setView();
  refresh();
})();
