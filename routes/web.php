<?php

use App\Http\Controllers\CustomAuthController;
use App\Http\Controllers\dashboardViewController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "web" middleware group. Make something great!
|
*/

/*Route::get('/', function () {
    return view('login');
});
*/

Route::get('/', [CustomAuthController::class, 'login'])->middleware('alreadyLoggedIn');
Route::get('/register', [CustomAuthController::class, 'register']);

// Post user data to database
Route::post('/registration', [CustomAuthController::class, 'registration'])->name('registration');

// Log user in
Route::post('/login-user', [CustomAuthController::class, 'loginUser'])->name('login-user');

Route::get('/home', [CustomAuthController::class, 'home'])->middleware('isLoggedIn');

Route::get('/dashboard', [dashboardViewController::class, 'dashboard@index'])->middleware('isLoggedIn');

/*
Route::get(‘blade’, function () {

    return view(‘demo’);

});
*/