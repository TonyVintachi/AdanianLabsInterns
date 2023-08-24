<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Kirivan - Login</title>
<link href="{{ asset('/css/login.css') }}" rel="stylesheet">
</head>
<body>
  <div class="signin-container">
    <img src="/images/kirivan_logo.png" class="logo">

    <form>

      <div class="input-group">

        <input type="email" id="email" name="email" required placeholder="Email Address">
      </div>
      <br>
      <div class="input-group">

        <input type="password" id="password" name="password" required placeholder="Password">
      </div>
      <br>
      <button class="login-button" type="submit">
        <span class="button-sticker">🔑</span>
        Login
      </button>

      <div class="login-actions">
        <div class="remember-me">
          <input type="checkbox" id="remember" name="remember">
          <label for="remember">Remember Me</label>
        </div>
        <div class="forgot-password">
          <a href="#">Forgot Password?</a>
        </div>
      </div>

    </form>
  </div>
</body>
</html>
