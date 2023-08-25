<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Kirivan - Register</title>
<link href="{{ asset('/css/register.css') }}" rel="stylesheet">
</head>
<body>
  <div class="signin-container">
    <img src="/images/kirivan_logo.png" class="logo">

    <form action="{{route('registration')}}" method="POST">

        @if (Session::has('success'))
            <div class=" alert alert-success">{{Session::get('success')}}</div>
        @else
            <div class=" alert alert-danger">{{Session::get('error')}}</div>
        @endif

        @csrf
        <h2>Add User</h2>

        <div class="input-group">
            <input type="first_name" id="first_name" name="first_name" required placeholder="First Name">
        </div>
        <br>

        <div class="input-group">
            <input type="last_name" id="last_name" name="last_name" required placeholder="Last Name">
        </div>
        <br>

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
            Add User
        </button>
    </form>
  </div>
</body>
</html>
