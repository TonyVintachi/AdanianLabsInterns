<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\User;
use Hash;
Use Session;

class CustomAuthController extends Controller
{
    //
    public function login(){
        return view("auth.login");
    }

    public function register(){
        return view("auth.register");

    }

    // Adds a new user to the database.
    public function registration(Request $request){
        $request -> validate([
            'first_name' => 'required',
            'last_name' => 'required',
            'email' => 'required|email|unique:users',
            'password' => 'required|min:8|max:15',
        ]);

        $user = new User();
        $user ->first_name = $request->first_name;
        $user ->last_name = $request->last_name;
        $user ->email = $request->email;
        $user ->password = Hash::make($request->password);

        //Save user data
        $res = $user -> save();

        // Get Response
        if ($res){
            // If response is successfull display message
            return  back()-> with('success', 'User registered successfully');
        } else {
            // If response was not successful display error
            return  back()-> with('error', 'Something wrong');
        }
    }

    public function loginUser(Request $request){
        $request -> validate([
            'email' => 'required|email',
            'password' => 'required|min:8|max:15',
        ]);

        $user = User::where('email', '=', $request -> email)-> first();

        if ($user){
            //If email exists and encrypted password is equal to the provided password
            if (Hash::check($request->password, $user ->password)){
                $request -> Session() -> put('loginId', - $user->id);
                // Redirect to home page
                return redirect('home');
            } else {
                // If encrypted password is not equal to the provided password\
                return  back()-> with('error', 'Incorrect Password');
            }

        } else {
            // If email doesnt exist
            return  back()-> with('error', 'Email not registered.');
        }

    }

    public function home(){

        $data = array();

        // Check if user is logged in, if so restore session
        if (Session::has('loginId')){
            $data = Session() -> put('loginId', - $user->id);
        }

        return view("home", compact(('data')));
    }

}
