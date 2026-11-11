// Back button: the page's Content-Security-Policy forbids inline scripts, so this small file does it.
document.addEventListener('click', function (e) {
  var b = e.target.closest('[data-back]');
  if (!b) return;
  if (window.history.length > 1) window.history.back(); else window.location.href = '/home';
});
