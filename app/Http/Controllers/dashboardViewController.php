<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
use App\Http\Requests;
use App\Http\Controllers\Controller;


class dashboardViewController extends Controller {
public function index()
{
$projects = DB::select('select * from projects');
return view('dashboard',['projects'=>$projects]);
}

//ignore this https://www.geeksforgeeks.org/different-ways-for-passing-data-to-view-in-laravel/
//swithing machince fixing a bug 

}