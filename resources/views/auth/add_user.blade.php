<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Kirivan - Login</title>
<link href="{{ asset('/css/add_user.css') }}" rel="stylesheet">
</head>
<body>
  <div class="signin-container">
    <form>
        <div class="input-group">
            <input type="name" id="name" name="name" required placeholder="Name">
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
