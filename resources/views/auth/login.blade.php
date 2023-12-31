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

    <form action="{{route('login-user')}}" method="POST">

        @if (Session::has('success'))
            <div class=" alert alert-success">{{Session::get('success')}}</div>
        @else
            <div class=" alert alert-danger">{{Session::get('error')}}</div>
        @endif

        @csrf
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
