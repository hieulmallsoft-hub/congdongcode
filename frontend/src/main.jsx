import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  BookOpenCheck,
  Code2,
  Flame,
  LogIn,
  LogOut,
  MessageCircle,
  MessageSquarePlus,
  RefreshCw,
  Search,
  Send,
  Sparkles,
  Tag,
  Users,
  X,
} from 'lucide-react';
import './styles.css';

const API_URL = 'http://localhost:8080/api';

const emptyPost = {
  title: '',
  body: '',
  codeSnippet: '',
  language: 'Java',
  tags: '',
  category: 'BUG',
};

function App() {
  const [authMode, setAuthMode] = useState('login');
  const [authOpen, setAuthOpen] = useState(false);
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [user, setUser] = useState(JSON.parse(localStorage.getItem('user') || 'null'));
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [postForm, setPostForm] = useState(emptyPost);
  const [answerForms, setAnswerForms] = useState({});
  const [submission, setSubmission] = useState({ answerCode: '', notes: '' });
  const [dailyChallenge, setDailyChallenge] = useState(null);
  const [posts, setPosts] = useState([]);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const totalAnswers = useMemo(
    () => posts.reduce((sum, post) => sum + Number(post.answerCount || 0), 0),
    [posts],
  );

  useEffect(() => {
    loadCommunity();
  }, [token]);

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
    return response.json();
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
      setAuthOpen(false);
      setMessage('Dang nhap thanh cong');
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadCommunity() {
    setLoading(true);
    try {
      const [challengeData, feedData] = await Promise.all([
        request('/community/challenges/daily'),
        request('/community/feed'),
      ]);
      setDailyChallenge(challengeData);
      setPosts(feedData);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function createPost(event) {
    event.preventDefault();
    if (!token) {
      openAuth('Dang nhap de dang bai len cong dong');
      return;
    }
    await action(async () => {
      await request('/community/posts', {
        method: 'POST',
        body: JSON.stringify(postForm),
      });
      setPostForm(emptyPost);
      await loadCommunity();
      return 'Da dang cau hoi len cong dong';
    });
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
      await loadCommunity();
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
    if (clearMessage) setMessage('');
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
            <span>Tim loi code, bai tap, ngon ngu...</span>
          </div>
          <div className="nav-actions">
            <button className="icon-button" onClick={loadCommunity} title="Tai lai">
              <RefreshCw size={18} />
            </button>
            {token ? (
              <>
                <span className="user-pill">{user?.fullName}</span>
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

        <section className="main-grid">
          <section className="feed-column">
            <div className="section-bar">
              <div>
                <p className="eyebrow">Discussion</p>
                <h2>Bai dang cong dong</h2>
              </div>
              <button className="ghost-button" onClick={loadCommunity}>
                <RefreshCw size={16} />
                Lam moi
              </button>
            </div>

            {posts.map((post) => (
              <article className="post-card" key={post.id}>
                <div className="post-main">
                  <div className="vote-rail">
                    <span>{post.answerCount}</span>
                    <small>answers</small>
                  </div>
                  <div className="post-content">
                    <div className="post-meta">
                      <span className={`category ${post.category.toLowerCase()}`}>{categoryLabel(post.category)}</span>
                      <span>{post.language}</span>
                      <span>{formatDateTime(post.createdAt)}</span>
                    </div>
                    <h3>{post.title}</h3>
                    <p>{post.body}</p>
                    {post.tags && (
                      <div className="tags">
                        {post.tags.split(',').map((tag) => <span key={tag}><Tag size={13} />{tag.trim()}</span>)}
                      </div>
                    )}
                    {post.codeSnippet && <pre><code>{post.codeSnippet}</code></pre>}

                    <div className="answers">
                      {post.answers.map((answer) => (
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
            ))}
            {!posts.length && (
              <div className="empty-card">
                <MessageCircle size={30} />
                <strong>Chua co bai dang</strong>
                <span>Hay la nguoi dau tien chia se loi code hoac bai tap kho.</span>
              </div>
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
                  Code lien quan
                  <textarea
                    rows="5"
                    value={postForm.codeSnippet}
                    onChange={(event) => setPostForm({ ...postForm, codeSnippet: event.target.value })}
                  />
                </label>
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

createRoot(document.getElementById('root')).render(<App />);
