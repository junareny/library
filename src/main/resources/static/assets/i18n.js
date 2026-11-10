(() => {
  'use strict';
  const english = {
    探索不同的知识世界: 'Explore a world of knowledge',
    '搜索书名，发现新知…': 'Search titles, discover more…',
    '。': '.',
    关闭提示: 'Dismiss notification',
    关闭: 'Close',
    退出: 'Sign out',
    '登录 / 注册': 'Sign in / Register',
    本: 'books',
    '/ 5 本': '/ 5 books',
    登录后开启阅读之旅: 'Sign in to start reading',
    我的借阅: 'My loans',
    探索书库: 'Explore books',
    发现好书: 'Discover books',
    '按借阅时间倒序 · 最多同时借阅 5 本': 'Newest loans first · Up to 5 active loans',
    按馆藏顺序排列: 'Sorted by catalog order',
    '你的阅读旅程，从这里开始': 'Your reading journey starts here',
    '登录后，即可查看借阅中的图书与归还日期。': 'Sign in to see your loans and due dates.',
    正在加载: 'Loading',
    暂时无法加载: 'Unable to load',
    重新加载: 'Try again',
    书架正等着你的下一本书: 'Your next book is waiting',
    '去书库逛逛，把好奇的世界带回来。': 'Explore the collection and find something new.',
    '登录，找到你正在读的书': 'Sign in to find your books',
    '登录后可筛选已借阅书籍，或管理你的借阅。':
      'Sign in to filter borrowed books and manage your loans.',
    搜索结果: 'search results',
    书籍: 'books',
    还没找到这本书: 'No matching books',
    这里暂时没有书籍: 'No books here yet',
    '试试其他书名关键词，或清除搜索查看全部馆藏。':
      'Try another title or clear the search to browse all books.',
    '挑一本感兴趣的书，开启下一段阅读。': 'Choose a book and start your next chapter.',
    '换一个筛选条件，看看其他好书。': 'Try a different filter to find more books.',
    查看全部书籍: 'Browse all books',
    上一页: 'Previous page',
    下一页: 'Next page',
    正在借阅: 'On loan to you',
    暂时借罄: 'All copies on loan',
    已借阅: 'Borrowed',
    借阅: 'Borrow',
    '借阅中…': 'Borrowing…',
    暂无库存: 'Unavailable',
    '已逾期 · 请及时归还': 'Overdue · Please return',
    借阅中: 'On loan',
    借阅日期: 'Borrowed',
    应还日期: 'Due date',
    归还图书: 'Return book',
    逾期仍占用借阅额度: 'Overdue loans still count toward your limit',
    欢迎回到阅间: 'Welcome back to Yuejian',
    开启你的阅读旅程: 'Start your reading journey',
    '登录，继续你的阅读旅程。': 'Sign in to continue reading.',
    '创建一个账号，让好书与你相遇。': 'Create an account and discover your next book.',
    登录: 'Sign in',
    注册并登录: 'Register and sign in',
    '还没有账号？': 'New here? ',
    '已经有账号？': 'Already have an account? ',
    立即注册: 'Register',
    返回登录: 'Sign in',
    显示: 'Show',
    隐藏: 'Hide',
    显示密码: 'Show password',
    隐藏密码: 'Hide password',
    '用户名需要 3–50 个字符，且不能包含空白字符。':
      'Username must be 3–50 characters with no whitespace.',
    '密码至少 8 个字符，且最多 72 个 UTF-8 字节。':
      'Password must be at least 8 characters and at most 72 UTF-8 bytes.',
    '请稍候…': 'Please wait…',
    '注册成功，欢迎来到阅间。挑一本书，开启阅读吧。':
      'Registration successful. Welcome to Yuejian!',
    暂无: 'Not available',
    馆藏数量: 'Total copies',
    当前可借: 'Available copies',
    '每次借阅 30 天，最多可同时借阅 5 本。请按时归还，让好书继续流转。':
      'Loans last 30 days, with up to 5 active loans. Please return books on time.',
    已在你的借阅中: 'Already in your loans',
    借阅这本书: 'Borrow this book',
    暂无可借副本: 'No copies available',
    图书: 'Book',
    确认归还: 'Confirm return',
    '归还中…': 'Returning…',
    '归还成功。谢谢你，让好书继续被阅读。':
      'Book returned. Thank you for sharing the joy of reading.',
    '已退出登录，仍可继续浏览书库。': 'Signed out. You can still browse the collection.',
    '登录已过期，请重新登录后继续。': 'Your session has expired. Please sign in again.',
    '用户名或密码不正确，请检查后重试。': 'Invalid username or password. Please try again.',
    '无法连接服务。操作结果可能尚未确认，请刷新借阅列表后再试。':
      'Unable to connect. The operation may have completed; refresh your loans before retrying.',
    '请求未完成，请稍后重试。': 'The request could not be completed. Please try again later.',
    '这个用户名已被使用，请换一个试试。': 'Username already exists. Please choose another.',
    '这本书暂时没有可借副本，可以看看其他书。': 'No copies available. Please choose another book.',
    '你已经借阅了这本书，可在“我的借阅”中查看。': 'You already have this book. Check My loans.',
    '最多可同时借阅 5 本书，请先归还一本。':
      'You can have up to 5 active loans. Return a book first.',
    '这本书已经归还，借阅列表已更新。':
      'This book has already been returned. Your loans have been updated.',
    '没有找到这条记录，请刷新后再试。': 'Record not found. Please refresh and try again.',
    '请检查填写内容。用户名为 3–50 个字符，密码至少 8 个字符。':
      'Check your input. Usernames need 3–50 characters and passwords at least 8.',
    '服务暂时不可用，请稍后重试。':
      'The service is temporarily unavailable. Please try again later.',
    '操作暂时未完成，请稍后重试。': 'The operation could not be completed. Please try again later.',
    逾期数量: '{count} overdue · Please return soon',
    借阅余量: 'You can borrow {count} more',
    借阅数量: '{count} active loans',
    结果数量: '{count} {kind}',
    分页范围: 'Showing {start}–{end} of {total} books',
    可借数量: '{count} available',
    书目编号: 'Book #{id}',
    剩余天数: '{count} days remaining',
    欢迎用户: 'Welcome back, {name}. Happy reading!',
    自动登录失败:
      'Your account was created, but automatic sign-in failed. {message} Switch to sign-in and try again.',
    借阅成功提示: 'Borrowed “{title}”. Please return it by {date}.',
    归还确认提示: 'Return “{title}”? This will free up one loan slot.',
    查看详情标签: 'View details for “{title}”',
    借阅按钮标签: '{action} “{title}”',
    语言: 'Language',
    '阅间 · 电子图书馆': 'Yuejian · Library',
    阅间: 'Yuejian',
    电子图书馆: 'Library',
    我的图书馆: 'My library',
    主导航: 'Main navigation',
    阅间首页: 'Yuejian home',
    '翻开一本书，': 'Open a book,',
    '遇见新的可能。': 'discover new possibilities.',
    '给好奇心，留一点时间。': 'Make time for curiosity.',
    '每一页，都是新开始': 'Every page is a new beginning',
    '阅间 / V1.0': 'Yuejian / V1.0',
    '让阅读，': 'Make reading',
    成为日常的一部分: 'part of your everyday life',
    '从一份好奇开始，寻找下一本值得翻开的书。': 'Follow your curiosity to your next great book.',
    '你的下一段旅程，就在书页之间。': 'Your next journey awaits between the pages.',
    发现你的下一本书: 'Find your next book',
    '为每一种好奇，找到答案': 'An answer for every curiosity',
    图书馆概览: 'Library overview',
    馆藏书目: 'Catalog',
    每次借阅: 'Loan period',
    30: '30',
    天: 'days',
    '从容阅读，按时归还': 'Read at your own pace. Return on time.',
    搜索书名: 'Search book titles',
    '搜索书名，发现你的下一本好书…': 'Search for your next great book…',
    搜索: 'Search',
    筛选书籍: 'Filter books',
    全部书籍: 'All books',
    有书可借: 'Available',
    我已借阅: 'Borrowed by me',
    让好书与你相遇: 'Find your next great book',
    '慢慢读，让每一次翻页都有收获。': 'Take your time. Make every page count.',
    关闭登录窗口: 'Close sign-in dialog',
    用户名: 'Username',
    密码: 'Password',
    '3–50 个字符，不含空格': '3–50 characters, no spaces',
    '至少 8 个字符': 'At least 8 characters',
    '密码至少 8 个字符，最多 72 个 UTF-8 字节。':
      'At least 8 characters and at most 72 UTF-8 bytes.',
    关闭书籍详情: 'Close book details',
    '这段阅读，告一段落': 'Ready to return this book?',
    再读一会儿: 'Keep reading',
    '请启用 JavaScript，以浏览图书和管理借阅。':
      'Enable JavaScript to browse books and manage loans.',
    '阅间电子图书馆，发现好书，管理你的借阅。':
      'Yuejian Library. Discover books and manage your loans.',
  };
  const chinese = {
    逾期数量: '{count} 本已逾期，请及时归还',
    借阅余量: '还可借阅 {count} 本',
    借阅数量: '{count} 本借阅中',
    结果数量: '{count} 本{kind}',
    分页范围: '显示 {start}–{end} 本，共 {total} 本',
    可借数量: '{count} 本可借',
    书目编号: '书目 #{id}',
    剩余天数: '剩余 {count} 天',
    欢迎用户: '欢迎回来，{name}。愿今天也有新的收获。',
    自动登录失败: '账号已创建，但自动登录未完成。{message} 请切换到登录后重试。',
    借阅成功提示: '借阅成功！《{title}》请于 {date} 前归还。',
    归还确认提示: '确定归还《{title}》吗？归还后会释放一个借阅名额。',
    查看详情标签: '查看《{title}》详情',
    借阅按钮标签: '{action}《{title}》',
  };
  function readLanguage() {
    try {
      const saved = localStorage.getItem('yuejian-language');
      if (saved === 'en' || saved === 'zh-CN') return saved;
    } catch {
      /* 存储不可用时使用默认中文，当前页面仍可切换语言。 */
    }
    return 'zh-CN';
  }
  let language = readLanguage();
  function t(key, values = {}) {
    const text = (language === 'en' ? english[key] : chinese[key]) ?? key;
    return String(text).replace(/\{(\w+)\}/g, (_, name) => String(values[name] ?? `{${name}}`));
  }
  function apply() {
    document.documentElement.lang = language;
    document.querySelectorAll('[data-i18n]').forEach((node) => {
      node.textContent = t(node.dataset.i18n);
    });
    for (const attr of ['placeholder', 'aria-label', 'content']) {
      document.querySelectorAll(`[data-i18n-${attr}]`).forEach((node) => {
        node.setAttribute(attr, t(node.getAttribute(`data-i18n-${attr}`)));
      });
    }
    document.querySelector('#language-select').value = language;
  }
  window.LibraryI18n = {
    get language() {
      return language;
    },
    t,
    setLanguage(value) {
      language = value === 'en' ? 'en' : 'zh-CN';
      try {
        localStorage.setItem('yuejian-language', language);
      } catch {
        /* 语言偏好无法保存时仅对当前页面生效。 */
      }
      apply();
    },
  };
  apply();
})();
