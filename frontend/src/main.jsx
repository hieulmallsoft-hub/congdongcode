import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  BookOpenCheck,
  Code2,
  Edit3,
  Flame,
  LogIn,
  LogOut,
  MessageCircle,
  MessageSquarePlus,
  Save,
  RefreshCw,
  Search,
  Send,
  Sparkles,
  Tag,
  Trash2,
  Users,
  X,
} from 'lucide-react';
import './styles.css';

const API_URL = 'http://localhost:8080/api';

const emptyPost = {
  title: '',
  body: '',
  codeSnippet: '',
  imageUrl: '',
  language: 'Java',
  tags: '',
  category: 'BUG',
};

const todayInputValue = () => new Date().toISOString().slice(0, 10);

const emptyChallenge = {
  title: '',
  prompt: '',
  difficulty: 'Easy',
  language: 'Java',
  starterCode: '',
  expectedOutput: '',
  solutionHint: '',
  publishDate: todayInputValue(),
};

function App() {
  const [view, setView] = useState('community');
  const [authMode, setAuthMode] = useState('login');
  const [authOpen, setAuthOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [user, setUser] = useState(JSON.parse(localStorage.getItem('user') || 'null'));
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [profileForm, setProfileForm] = useState({ fullName: '', avatarUrl: '' });
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '' });
  const [postForm, setPostForm] = useState(emptyPost);
  const [editingPostId, setEditingPostId] = useState(null);
  const [editPostForm, setEditPostForm] = useState(emptyPost);
  const [answerForms, setAnswerForms] = useState({});
  const [submission, setSubmission] = useState({ answerCode: '', notes: '' });
  const [challengeForm, setChallengeForm] = useState(emptyChallenge);
  const [dailyChallenge, setDailyChallenge] = useState(null);
  const [selectedPost, setSelectedPost] = useState(null);
  const [posts, setPosts] = useState([]);
  const [adminUsers, setAdminUsers] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (!isAdmin && view === 'admin') {
      setView('community');
    }
  }, [isAdmin, view]);

  const totalAnswers = useMemo(
    () => posts.reduce((sum, post) => sum + Number(post.answerCount || 0), 0),
    [posts],
  );

  const filteredPosts = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();
    if (!keyword) return posts;
    return posts.filter((post) => {
      const answers = (post.answers || [])
        .map((answer) => `${answer.authorName || ''} ${answer.content || ''} ${answer.codeSnippet || ''}`)
        .join(' ');
      const searchable = [
        post.title,
        post.body,
        post.codeSnippet,
        post.imageUrl,
        post.language,
        post.tags,
        post.category,
        answers,
      ].join(' ').toLowerCase();
      return searchable.includes(keyword);
    });
  }, [posts, searchQuery]);

  useEffect(() => {
    loadCommunity();
  }, [token]);

  useEffect(() => {
    if (!token) return;
    loadCurrentUser();
  }, [token]);

  useEffect(() => {
    setProfileForm({
      fullName: user?.fullName || '',
      avatarUrl: user?.avatarUrl || '',
    });
  }, [user]);

  async function request(path, options = {}) {
    let response;
    try {
      response = await fetch(`${API_URL}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...(options.headers || {}),
        },
      });
    } catch {
      throw new Error('Backend chua chay. Hay mo backend o http://localhost:8080');
    }

    if (!response.ok) {
      const error = await response.json().catch(() => null);
      if ((response.status === 401 || response.status === 403) && token) {
        logout(false);
        openAuth('Phien dang nhap het han. Hay dang nhap lai');
      }
      throw new Error(error?.message || `Loi API ${response.status}`);
    }

    if (response.status === 204) return null;
    const data = await response.json();
    if (data && typeof data === 'object' && 'success' in data && 'data' in data) {
      return data.data;
    }
    return data;
  }

  async function submitAuth(event) {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      const payload = authMode === 'register' ? form : { email: form.email, password: form.password };
      const data = await request(`/auth/${authMode}`, {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
      setToken(data.token);
      setUser(data.user);
      setAdminUsers([]);
      setAuthOpen(false);
      setMessage('Dang nhap thanh cong');
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadCurrentUser() {
    try {
      const currentUser = await request('/users/me');
      localStorage.setItem('user', JSON.stringify(currentUser));
      setUser(currentUser);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function loadCommunity() {
    setLoading(true);
    try {
      const [challengeResult, feedResult] = await Promise.allSettled([
        request('/community/challenges/daily'),
        request('/community/feed'),
      ]);
      if (challengeResult.status === 'fulfilled') {
        setDailyChallenge(challengeResult.value);
      } else {
        setDailyChallenge(null);
        if (!challengeResult.reason.message.includes('Chua co daily challenge')) {
          setMessage(challengeResult.reason.message);
        }
      }
      if (feedResult.status === 'fulfilled') {
        setPosts(feedResult.value);
      } else {
        setMessage(feedResult.reason.message);
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadAdminUsers() {
    if (!isAdmin) return;
    await action(async () => {
      const users = await request('/users');
      setAdminUsers(users);
      return 'Da tai danh sach tai khoan';
    });
  }

  async function openPostDetail(postId) {
    await action(async () => {
      const post = await request(`/community/posts/${postId}`);
      setSelectedPost(post);
      return '';
    });
  }

  function closePostDetail() {
    setSelectedPost(null);
    cancelEditPost();
  }

  async function updateProfile(event) {
    event.preventDefault();
    if (!token) return;
    await action(async () => {
      const updatedUser = await request('/users/me', {
        method: 'PUT',
        body: JSON.stringify(profileForm),
      });
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setUser(updatedUser);
      return 'Da cap nhat thong tin tai khoan';
    });
  }

  async function changePassword(event) {
    event.preventDefault();
    if (!token) return;
    await action(async () => {
      await request('/users/me/password', {
        method: 'PUT',
        body: JSON.stringify(passwordForm),
      });
      setPasswordForm({ currentPassword: '', newPassword: '' });
      return 'Da doi mat khau';
    });
  }

  async function createPost(event) {
    event.preventDefault();
    if (!token) {
      openAuth('Dang nhap de dang bai len cong dong');
      return;
    }
    await action(async () => {
      const createdPost = await request('/community/posts', {
        method: 'POST',
        body: JSON.stringify(postForm),
      });
      setPostForm(emptyPost);
      if (createdPost) {
        setPosts((currentPosts) => [createdPost, ...currentPosts]);
      } else {
        await loadCommunity();
      }
      return 'Da dang cau hoi len cong dong';
    });
  }

  function startEditPost(post) {
    setEditingPostId(post.id);
    setEditPostForm({
      title: post.title || '',
      body: post.body || '',
      codeSnippet: post.codeSnippet || '',
      imageUrl: post.imageUrl || '',
      language: post.language || 'Java',
      tags: post.tags || '',
      category: post.category || 'BUG',
    });
  }

  function cancelEditPost() {
    setEditingPostId(null);
    setEditPostForm(emptyPost);
  }

  async function updatePost(event, postId) {
    event.preventDefault();
    await action(async () => {
      const updatedPost = await request(`/community/posts/${postId}`, {
        method: 'PUT',
        body: JSON.stringify(editPostForm),
      });
      setPosts((currentPosts) =>
        currentPosts.map((post) => (post.id === postId ? { ...post, ...updatedPost } : post)),
      );
      if (selectedPost?.id === postId) {
        setSelectedPost(updatedPost);
      }
      cancelEditPost();
      return 'Da cap nhat bai dang';
    });
  }

  async function deletePost(postId) {
    if (!window.confirm('Xoa bai dang nay? Cac cau tra loi trong bai cung se bi xoa.')) return;
    await action(async () => {
      await request(`/community/posts/${postId}`, { method: 'DELETE' });
      setPosts((currentPosts) => currentPosts.filter((post) => post.id !== postId));
      if (selectedPost?.id === postId) {
        setSelectedPost(null);
      }
      if (editingPostId === postId) cancelEditPost();
      return 'Da xoa bai dang';
    });
  }

  function setPostImage(file, setFormState = setPostForm) {
    if (!file || !file.type.startsWith('image/')) {
      setMessage('Hay chon file anh hop le');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setMessage('Anh toi da 2MB');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setFormState((current) => ({ ...current, imageUrl: reader.result || '' }));
      setMessage('');
    };
    reader.onerror = () => setMessage('Khong doc duoc file anh');
    reader.readAsDataURL(file);
  }

  function handlePostImagePaste(event) {
    const imageItem = Array.from(event.clipboardData?.items || [])
      .find((item) => item.type.startsWith('image/'));
    if (!imageItem) return;
    event.preventDefault();
    setPostImage(imageItem.getAsFile());
  }

  function handleEditImagePaste(event) {
    const imageItem = Array.from(event.clipboardData?.items || [])
      .find((item) => item.type.startsWith('image/'));
    if (!imageItem) return;
    event.preventDefault();
    setPostImage(imageItem.getAsFile(), setEditPostForm);
  }

  async function addAnswer(event, postId) {
    event.preventDefault();
    if (!token) {
      openAuth('Dang nhap de comment hoac gui loi giai');
      return;
    }
    const current = answerForms[postId] || { content: '', codeSnippet: '' };
    await action(async () => {
      await request(`/community/posts/${postId}/answers`, {
        method: 'POST',
        body: JSON.stringify(current),
      });
      setAnswerForms((forms) => ({ ...forms, [postId]: { content: '', codeSnippet: '' } }));
      if (selectedPost?.id === postId) {
        const post = await request(`/community/posts/${postId}`);
        setSelectedPost(post);
        setPosts((currentPosts) =>
          currentPosts.map((currentPost) => (currentPost.id === postId ? { ...currentPost, ...post } : currentPost)),
        );
      } else {
        await loadCommunity();
      }
      return 'Da gui cau tra loi';
    });
  }

  async function submitChallenge(event) {
    event.preventDefault();
    if (!dailyChallenge) return;
    if (!token) {
      openAuth('Dang nhap de nop loi giai bai tap hom nay');
      return;
    }
    await action(async () => {
      await request(`/community/challenges/${dailyChallenge.id}/submissions`, {
        method: 'POST',
        body: JSON.stringify(submission),
      });
      setSubmission({ answerCode: '', notes: '' });
      await loadCommunity();
      return 'Da nop loi giai bai tap hom nay';
    });
  }

  async function saveDailyChallenge(event) {
    event.preventDefault();
    if (!isAdmin) return;
    await action(async () => {
      const savedChallenge = await request('/community/challenges', {
        method: 'POST',
        body: JSON.stringify(challengeForm),
      });
      if (savedChallenge?.publishDate === todayInputValue()) {
        setDailyChallenge(savedChallenge);
      }
      setChallengeForm({ ...emptyChallenge, publishDate: todayInputValue() });
      return 'Da luu daily challenge';
    });
  }

  async function action(callback) {
    setLoading(true);
    setMessage('');
    try {
      setMessage(await callback());
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  function openAuth(authMessage = '') {
    setAuthOpen(true);
    setMessage(authMessage);
  }

  function logout(clearMessage = true) {
    localStorage.clear();
    setToken('');
    setUser(null);
    setAdminUsers([]);
    setView('community');
    setProfileOpen(false);
    setPasswordForm({ currentPassword: '', newPassword: '' });
    if (clearMessage) setMessage('');
  }

  function renderPostCard(post, isDetail = false) {
    const canManagePost = token && (
      isAdmin ||
      Number(post.authorId) === Number(user?.id) ||
      (!post.authorId && post.authorName === user?.fullName)
    );
    const isEditing = editingPostId === post.id;

    return (
      <article className={`post-card ${isDetail ? 'detail-card' : ''}`} key={post.id}>
        <div className="post-main">
          {!isDetail && (
            <div className="vote-rail">
              <span>{post.answerCount}</span>
              <small>answers</small>
            </div>
          )}
          <div className="post-content">
            <div className="post-meta">
              <span className={`category ${post.category.toLowerCase()}`}>{categoryLabel(post.category)}</span>
              <span>{post.language}</span>
              <span>{formatDateTime(post.createdAt)}</span>
              {isDetail && <span>{post.answerCount} answers</span>}
            </div>
            {canManagePost && (
              <div className="post-actions">
                <button type="button" className="ghost-button" onClick={() => startEditPost(post)} disabled={loading || isEditing}>
                  <Edit3 size={15} />
                  Sua
                </button>
                <button type="button" className="ghost-button danger-button" onClick={() => deletePost(post.id)} disabled={loading}>
                  <Trash2 size={15} />
                  Xoa
                </button>
              </div>
            )}

            {isEditing ? (
              <form className="form edit-post-form" onSubmit={(event) => updatePost(event, post.id)}>
                <label>
                  Tieu de
                  <input
                    value={editPostForm.title}
                    onChange={(event) => setEditPostForm({ ...editPostForm, title: event.target.value })}
                    required
                  />
                </label>
                <div className="form-grid">
                  <label>
                    Loai
                    <select
                      value={editPostForm.category}
                      onChange={(event) => setEditPostForm({ ...editPostForm, category: event.target.value })}
                    >
                      <option value="BUG">Loi code</option>
                      <option value="EXERCISE">Bai tap kho</option>
                      <option value="DISCUSSION">Thao luan</option>
                    </select>
                  </label>
                  <label>
                    Ngon ngu
                    <input
                      value={editPostForm.language}
                      onChange={(event) => setEditPostForm({ ...editPostForm, language: event.target.value })}
                      required
                    />
                  </label>
                </div>
                <label>
                  Mo ta van de
                  <textarea
                    rows="4"
                    value={editPostForm.body}
                    onChange={(event) => setEditPostForm({ ...editPostForm, body: event.target.value })}
                    required
                  />
                </label>
                <label>
                  Text code loi
                  <textarea
                    rows="5"
                    value={editPostForm.codeSnippet}
                    onChange={(event) => setEditPostForm({ ...editPostForm, codeSnippet: event.target.value })}
                  />
                </label>
                <label>
                  Anh loi
                  <div className="image-picker" onPaste={handleEditImagePaste}>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(event) => setPostImage(event.target.files?.[0], setEditPostForm)}
                    />
                    <span>Chon anh moi hoac paste anh chup man hinh vao day</span>
                  </div>
                  <input
                    type="url"
                    value={editPostForm.imageUrl.startsWith('data:') ? '' : editPostForm.imageUrl}
                    onChange={(event) => setEditPostForm({ ...editPostForm, imageUrl: event.target.value })}
                    placeholder="Hoac dan URL anh https://..."
                  />
                </label>
                {editPostForm.imageUrl && (
                  <div className="image-preview">
                    <img src={editPostForm.imageUrl} alt="" />
                    <button type="button" className="ghost-button" onClick={() => setEditPostForm({ ...editPostForm, imageUrl: '' })}>
                      Xoa anh
                    </button>
                  </div>
                )}
                <label>
                  Tags
                  <input
                    value={editPostForm.tags}
                    onChange={(event) => setEditPostForm({ ...editPostForm, tags: event.target.value })}
                  />
                </label>
                <div className="edit-actions">
                  <button className="primary-button" disabled={loading}>
                    <Save size={17} />
                    Luu bai
                  </button>
                  <button type="button" className="ghost-button" onClick={cancelEditPost} disabled={loading}>
                    Huy
                  </button>
                </div>
              </form>
            ) : (
              <>
                {isDetail ? (
                  <h2 className="detail-title">{post.title}</h2>
                ) : (
                  <button type="button" className="post-title-button" onClick={() => openPostDetail(post.id)}>
                    {post.title}
                  </button>
                )}
                <p>{post.body}</p>
                {post.tags && (
                  <div className="tags">
                    {post.tags.split(',').map((tag) => <span key={tag}><Tag size={13} />{tag.trim()}</span>)}
                  </div>
                )}
                {post.imageUrl && (
                  <div className="post-image">
                    <img src={post.imageUrl} alt={post.title} loading="lazy" />
                  </div>
                )}
                {post.codeSnippet && <pre><code>{post.codeSnippet}</code></pre>}
              </>
            )}

            <div className="answers">
              {(post.answers || []).map((answer) => (
                <div className="answer" key={answer.id}>
                  <div className="answer-meta">{answer.authorName} - {formatDateTime(answer.createdAt)}</div>
                  <p>{answer.content}</p>
                  {answer.codeSnippet && <pre><code>{answer.codeSnippet}</code></pre>}
                </div>
              ))}
            </div>

            <form className="answer-form" onSubmit={(event) => addAnswer(event, post.id)}>
              <textarea
                rows="2"
                placeholder="Them comment hoac goi y loi giai..."
                value={(answerForms[post.id] || {}).content || ''}
                onChange={(event) =>
                  setAnswerForms((forms) => ({
                    ...forms,
                    [post.id]: { ...(forms[post.id] || {}), content: event.target.value },
                  }))
                }
                required
              />
              <textarea
                rows="2"
                placeholder="Code minh hoa"
                value={(answerForms[post.id] || {}).codeSnippet || ''}
                onChange={(event) =>
                  setAnswerForms((forms) => ({
                    ...forms,
                    [post.id]: { ...(forms[post.id] || {}), codeSnippet: event.target.value },
                  }))
                }
              />
              <button disabled={loading}>
                <Send size={16} />
                Tra loi
              </button>
            </form>
          </div>
        </div>
      </article>
    );
  }

  return (
    <main>
      <header className="nav">
        <div className="nav-inner">
          <div className="brand">
            <span className="brand-icon"><Code2 size={21} /></span>
            <strong>CodeTogether</strong>
          </div>
          <div className="nav-search">
            <Search size={17} />
            <input
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Tim cau hoi, loi code, ngon ngu..."
              aria-label="Tim cau hoi"
            />
            {searchQuery && (
              <button className="search-clear" onClick={() => setSearchQuery('')} title="Xoa tim kiem">
                <X size={15} />
              </button>
            )}
          </div>
          <div className="nav-actions">
            <button className="icon-button" onClick={loadCommunity} title="Tai lai">
              <RefreshCw size={18} />
            </button>
            {token ? (
              <>
                {isAdmin && (
                  <button className="ghost-button" onClick={() => setView(view === 'admin' ? 'community' : 'admin')}>
                    <BookOpenCheck size={17} />
                    {view === 'admin' ? 'Cong dong' : 'Admin'}
                  </button>
                )}
                <button className="user-pill" onClick={() => setProfileOpen(true)} title="Thong tin tai khoan">
                  {user?.fullName}
                  {user?.role && <small>{user.role}</small>}
                </button>
                <button className="icon-button" onClick={() => logout()} title="Dang xuat">
                  <LogOut size={18} />
                </button>
              </>
            ) : (
              <button className="login-button" onClick={() => openAuth()}>
                <LogIn size={18} />
                Dang nhap
              </button>
            )}
          </div>
        </div>
      </header>

      <section className="hero-strip">
        <div>
          <p className="eyebrow">Home feed</p>
          <h1>Cong dong hoi dap va luyen code moi ngay</h1>
          <p>Xem bai dang cong khai. Khi ban dang bai, comment hoac nop loi giai, he thong se yeu cau dang nhap.</p>
        </div>
        <div className="scoreboard">
          <div><strong>{posts.length}</strong><span>Bai dang</span></div>
          <div><strong>{totalAnswers}</strong><span>Tra loi</span></div>
          <div><strong>{dailyChallenge?.difficulty || '-'}</strong><span>Daily</span></div>
        </div>
      </section>

      <div className="app-shell">
        {message && <div className="notice">{message}</div>}

        {view === 'admin' && isAdmin ? (
          <section className="admin-dashboard">
            <div className="section-bar">
              <div>
                <p className="eyebrow">Admin</p>
                <h2>Bang dieu khien quan tri</h2>
              </div>
              <button className="ghost-button" onClick={() => setView('community')}>
                Quay lai cong dong
              </button>
            </div>

            <div className="admin-grid">
              <article className="panel admin-panel">
                <div className="panel-heading">
                  <h2><Users size={19} /> Quan tri user</h2>
                  <button className="ghost-button" onClick={loadAdminUsers} disabled={loading}>
                    <RefreshCw size={16} />
                    Tai
                  </button>
                </div>
                {adminUsers.length ? (
                  <div className="admin-users">
                    {adminUsers.map((account) => (
                      <div className="admin-user" key={account.id}>
                        <div>
                          <strong>{account.fullName}</strong>
                          <span>{account.email}</span>
                        </div>
                        <span className={`role-badge ${account.role.toLowerCase()}`}>{account.role}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="empty">Bam tai de xem danh sach tai khoan.</p>
                )}
              </article>

              <article className="panel admin-challenge-panel">
                <div className="panel-heading">
                  <h2><BookOpenCheck size={19} /> Dang Daily</h2>
                </div>
                <form className="form compact" onSubmit={saveDailyChallenge}>
                  <label>
                    Ngay dang
                    <input
                      type="date"
                      value={challengeForm.publishDate}
                      onChange={(event) => setChallengeForm({ ...challengeForm, publishDate: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Tieu de
                    <input
                      value={challengeForm.title}
                      onChange={(event) => setChallengeForm({ ...challengeForm, title: event.target.value })}
                      placeholder="Two Sum bien the"
                      required
                    />
                  </label>
                  <div className="form-grid">
                    <label>
                      Do kho
                      <select
                        value={challengeForm.difficulty}
                        onChange={(event) => setChallengeForm({ ...challengeForm, difficulty: event.target.value })}
                      >
                        <option value="Easy">Easy</option>
                        <option value="Medium">Medium</option>
                        <option value="Hard">Hard</option>
                      </select>
                    </label>
                    <label>
                      Ngon ngu
                      <input
                        value={challengeForm.language}
                        onChange={(event) => setChallengeForm({ ...challengeForm, language: event.target.value })}
                        required
                      />
                    </label>
                  </div>
                  <label>
                    De bai
                    <textarea
                      rows="4"
                      value={challengeForm.prompt}
                      onChange={(event) => setChallengeForm({ ...challengeForm, prompt: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Starter code
                    <textarea
                      rows="5"
                      value={challengeForm.starterCode}
                      onChange={(event) => setChallengeForm({ ...challengeForm, starterCode: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Expected output
                    <textarea
                      rows="2"
                      value={challengeForm.expectedOutput}
                      onChange={(event) => setChallengeForm({ ...challengeForm, expectedOutput: event.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Goi y
                    <textarea
                      rows="2"
                      value={challengeForm.solutionHint}
                      onChange={(event) => setChallengeForm({ ...challengeForm, solutionHint: event.target.value })}
                    />
                  </label>
                  <button className="primary-button" disabled={loading}>
                    <Save size={18} />
                    Luu daily challenge
                  </button>
                </form>
              </article>
            </div>
          </section>
        ) : (
        <section className="main-grid">
          <section className="feed-column">
            <div className="section-bar">
              <div>
                <p className="eyebrow">Discussion</p>
                <h2>{selectedPost ? 'Chi tiet bai dang' : searchQuery.trim() ? `Ket qua tim kiem (${filteredPosts.length})` : 'Bai dang cong dong'}</h2>
              </div>
              {selectedPost ? (
                <button className="ghost-button" onClick={closePostDetail}>
                  Quay lai
                </button>
              ) : (
                <button className="ghost-button" onClick={loadCommunity}>
                  <RefreshCw size={16} />
                  Lam moi
                </button>
              )}
            </div>

            {selectedPost ? (
              renderPostCard(selectedPost, true)
            ) : (
              <>
                {filteredPosts.map((post) => renderPostCard(post))}
                {!filteredPosts.length && (
              <div className="empty-card">
                <MessageCircle size={30} />
                <strong>{searchQuery.trim() ? 'Khong tim thay cau hoi' : 'Chua co bai dang'}</strong>
                <span>{searchQuery.trim() ? 'Thu tu khoa khac hoac xoa tim kiem de xem tat ca.' : 'Hay la nguoi dau tien chia se loi code hoac bai tap kho.'}</span>
              </div>
                )}
              </>
            )}
          </section>

          <aside className="right-rail">
            <article className="panel challenge-panel">
              <div className="panel-heading">
                <h2><Flame size={19} /> Daily Challenge</h2>
                {dailyChallenge && <span className="pill">{formatDate(dailyChallenge.publishDate)}</span>}
              </div>
              {dailyChallenge ? (
                <>
                  <h3>{dailyChallenge.title}</h3>
                  <p>{dailyChallenge.prompt}</p>
                  <div className="meta-row">
                    <span>{dailyChallenge.language}</span>
                    <span>{dailyChallenge.difficulty}</span>
                    <span>{dailyChallenge.submissionCount} loi giai</span>
                  </div>
                  <pre><code>{dailyChallenge.starterCode}</code></pre>
                  <div className="expected">{dailyChallenge.expectedOutput}</div>
                  <details>
                    <summary>Goi y</summary>
                    <p>{dailyChallenge.solutionHint}</p>
                  </details>
                  <form className="form compact" onSubmit={submitChallenge}>
                    <label>
                      Loi giai cua ban
                      <textarea
                        rows="5"
                        value={submission.answerCode}
                        onChange={(event) => setSubmission({ ...submission, answerCode: event.target.value })}
                        required
                      />
                    </label>
                    <label>
                      Ghi chu
                      <textarea
                        rows="2"
                        value={submission.notes}
                        onChange={(event) => setSubmission({ ...submission, notes: event.target.value })}
                      />
                    </label>
                    <button className="primary-button" disabled={loading}>
                      <Send size={18} />
                      Nop loi giai
                    </button>
                  </form>
                </>
              ) : (
                <p className="empty">Chua co bai tap nao.</p>
              )}
            </article>

            <article className="panel composer-panel">
              <div className="panel-heading">
                <h2><MessageSquarePlus size={19} /> Dang bai moi</h2>
              </div>
              <form className="form compact" onSubmit={createPost}>
                <label>
                  Tieu de
                  <input
                    value={postForm.title}
                    onChange={(event) => setPostForm({ ...postForm, title: event.target.value })}
                    placeholder="NullPointerException khi doc file"
                    required
                  />
                </label>
                <div className="form-grid">
                  <label>
                    Loai
                    <select
                      value={postForm.category}
                      onChange={(event) => setPostForm({ ...postForm, category: event.target.value })}
                    >
                      <option value="BUG">Loi code</option>
                      <option value="EXERCISE">Bai tap kho</option>
                      <option value="DISCUSSION">Thao luan</option>
                    </select>
                  </label>
                  <label>
                    Ngon ngu
                    <input
                      value={postForm.language}
                      onChange={(event) => setPostForm({ ...postForm, language: event.target.value })}
                      required
                    />
                  </label>
                </div>
                <label>
                  Mo ta van de
                  <textarea
                    rows="4"
                    value={postForm.body}
                    onChange={(event) => setPostForm({ ...postForm, body: event.target.value })}
                    required
                  />
                </label>
                <label>
                  Text code loi
                  <textarea
                    rows="5"
                    value={postForm.codeSnippet}
                    onChange={(event) => setPostForm({ ...postForm, codeSnippet: event.target.value })}
                    placeholder="Paste stack trace, error log hoac doan code loi"
                  />
                </label>
                <label>
                  Anh loi
                  <div className="image-picker" onPaste={handlePostImagePaste}>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(event) => setPostImage(event.target.files?.[0])}
                    />
                    <span>Chon anh tu may hoac paste anh chup man hinh vao day</span>
                  </div>
                  <input
                    type="url"
                    value={postForm.imageUrl.startsWith('data:') ? '' : postForm.imageUrl}
                    onChange={(event) => setPostForm({ ...postForm, imageUrl: event.target.value })}
                    placeholder="Hoac dan URL anh https://..."
                  />
                </label>
                {postForm.imageUrl && (
                  <div className="image-preview">
                    <img src={postForm.imageUrl} alt="" />
                    <button type="button" className="ghost-button" onClick={() => setPostForm({ ...postForm, imageUrl: '' })}>
                      Xoa anh
                    </button>
                  </div>
                )}
                <label>
                  Tags
                  <input
                    value={postForm.tags}
                    onChange={(event) => setPostForm({ ...postForm, tags: event.target.value })}
                    placeholder="java, array, spring"
                  />
                </label>
                <button className="primary-button" disabled={loading}>
                  <MessageSquarePlus size={18} />
                  Dang len cong dong
                </button>
              </form>
            </article>
          </aside>
        </section>
        )}
      </div>

      {authOpen && (
        <div className="auth-modal" role="dialog" aria-modal="true">
          <section className="auth-card">
            <button className="close-button" onClick={() => setAuthOpen(false)} title="Dong">
              <X size={18} />
            </button>
            <div className="brand-icon modal-brand">
              <Code2 size={24} />
            </div>
            <h2>{authMode === 'login' ? 'Dang nhap' : 'Tao tai khoan'}</h2>
            <p>Doc bai dang khong can tai khoan. Tai khoan chi dung khi ban muon dong gop noi dung.</p>
            <div className="tabs">
              <button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>
                Dang nhap
              </button>
              <button className={authMode === 'register' ? 'active' : ''} onClick={() => setAuthMode('register')}>
                Dang ky
              </button>
            </div>
            <form onSubmit={submitAuth} className="form">
              {authMode === 'register' && (
                <label>
                  Ho ten
                  <input
                    value={form.fullName}
                    onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                    required
                  />
                </label>
              )}
              <label>
                Email
                <input
                  type="email"
                  value={form.email}
                  onChange={(event) => setForm({ ...form, email: event.target.value })}
                  required
                />
              </label>
              <label>
                Mat khau
                <input
                  type="password"
                  minLength={6}
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                  required
                />
              </label>
              <button className="primary-button" disabled={loading}>
                {loading ? 'Dang xu ly...' : authMode === 'login' ? 'Dang nhap' : 'Tao tai khoan'}
              </button>
            </form>
          </section>
        </div>
      )}

      {profileOpen && token && (
        <div className="auth-modal" role="dialog" aria-modal="true">
          <section className="auth-card profile-card">
            <button className="close-button" onClick={() => setProfileOpen(false)} title="Dong">
              <X size={18} />
            </button>
            <div className="panel-heading profile-heading">
              <h2><Users size={20} /> Tai khoan</h2>
            </div>
            <div className="account-summary">
              <div className="avatar-preview">
                {user?.avatarUrl ? <img src={user.avatarUrl} alt="" /> : <span>{initials(user?.fullName)}</span>}
              </div>
              <div>
                <strong>{user?.fullName}</strong>
                <span>{user?.email}</span>
              </div>
            </div>
            <form className="form compact" onSubmit={updateProfile}>
              <label>
                Ho ten
                <input
                  value={profileForm.fullName}
                  onChange={(event) => setProfileForm({ ...profileForm, fullName: event.target.value })}
                  required
                />
              </label>
              <label>
                Avatar URL
                <input
                  type="url"
                  value={profileForm.avatarUrl}
                  onChange={(event) => setProfileForm({ ...profileForm, avatarUrl: event.target.value })}
                  placeholder="https://..."
                />
              </label>
              <button className="primary-button" disabled={loading}>
                <Save size={18} />
                Luu thong tin
              </button>
            </form>
            <form className="form compact password-form" onSubmit={changePassword}>
              <label>
                Mat khau hien tai
                <input
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={(event) => setPasswordForm({ ...passwordForm, currentPassword: event.target.value })}
                  required
                />
              </label>
              <label>
                Mat khau moi
                <input
                  type="password"
                  minLength={8}
                  value={passwordForm.newPassword}
                  onChange={(event) => setPasswordForm({ ...passwordForm, newPassword: event.target.value })}
                  required
                />
              </label>
              <button className="ghost-button" disabled={loading}>
                Doi mat khau
              </button>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}

function categoryLabel(category) {
  if (category === 'BUG') return 'Loi code';
  if (category === 'EXERCISE') return 'Bai tap kho';
  return 'Thao luan';
}

function formatDate(value) {
  return new Date(value).toLocaleDateString('vi-VN');
}

function formatDateTime(value) {
  return new Date(value).toLocaleString('vi-VN');
}

function initials(name = '') {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  return parts.slice(0, 2).map((part) => part[0]?.toUpperCase()).join('');
}

createRoot(document.getElementById('root')).render(<App />);
