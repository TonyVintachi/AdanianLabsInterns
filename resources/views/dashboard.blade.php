<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trello-like Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.1/css/all.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/dragula/3.7.2/dragula.min.css">

    <style>
        /* Resetting default styles */
        body, html {
            margin: 0;
            padding: 0;
        }

        /* Styles for the Top Bar */
        .top-bar {
            background-color: #e8a452;
            color: #ecf0f1;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 20px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .options-icon {
            margin-right: 20px;
            cursor: pointer;
        }

        .brand-name {
            font-weight: bold;
            font-size: 1.5rem;
            margin-right: auto;
        }

        .search-box {
            flex: 0;
            padding: 10px 10px;
            margin: 0 15px;
            border: none;
            border-radius: 4px;
            background-color: #fff;
            order: 2;
        }

        .notification-icon {
            cursor: pointer;
            order: 3;
        }

        /* Styles for Navbar */
        .navbar {
            background-color: #e8a452;
            padding: 20px;
            width: 230px;
            position: fixed;
            height: calc(100% - 50px);
            left: 0;
            top: 50px;
            box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
            color: #ecf0f1;
        }

        .navbar a {
            display: block;
            padding: 10px 20px;
            color: #ecf0f1;
            text-decoration: none;
            transition: background-color 0.3s;
            border-radius: 5px;
        }

        .navbar a:hover {
            background-color: #34495e;
        }

        .profile-card {
            text-align: center;
            margin-bottom: 30px;
        }

        .profile-card .profile-icon-label {
            display: inline-block;
            background-color: #a2b4c5;
            padding: 15px;
            border-radius: 50%;
            transition: background-color 0.3s;
        }

        .profile-card .profile-icon-label:hover {
            background-color: #bbcada;
        }

        .profile-icon-label i {
            font-size: 3.5em;
            cursor: pointer;
        }

        .profile-card div {
            margin-top: 10px;
            font-size: 1.1em;
        }

        /* Styles for Trello-like dashboard */
        .dashboard {
            display: flex;
            gap: 20px;
            padding: 20px;
            margin-left: 250px;
        }

        .list {
            background-color: #ecf0f1;
            border-radius: 8px;
            width: 250px;
            padding: 10px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .list-title {
            font-weight: bold;
            font-size: 1.5rem;
            margin-bottom: 10px;
            text-align: center;
        }

        .card {
            background-color: #ffffff;
            padding: 10px;
            margin-bottom: 10px;
            border-radius: 4px;
            cursor: move;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
    </style>
</head>

<body>
    <!-- Top Bar -->
    <div class="top-bar">
        <i class="fas fa-bars options-icon"></i>
        <span class="brand-name">Kirivan</span>
        <input type="search" placeholder="Search..." class="search-box">
        <i class="fas fa-bell notification-icon"></i>
    </div>

    <!-- Navigation Bar -->
    <div class="navbar">
        <div class="profile-card">
            <div class="profile-icon-label">
                <i class="fas fa-user-circle"></i>
            </div>
            <div>Username</div>
        </div>
        <a href="homepage.html">Home</a>
        <a href="dashboard.html">Dashboard</a>
        <a href="project.html">Projects</a>
        <a href="task.html">Tasks</a>
        <a href="#">Settings</a>
    </div>

    <!-- Trello-like dashboard content -->
    <div class="dashboard">
        <!-- To Do List -->
        <div class="list">
            <div class="list-title">To Do</div>
            <div class="card">Task 1</div>
            <div class="card">Task 2</div>
        </div>

        <!-- In Progress List -->
        <div class="list">
            <div class="list-title">In Progress</div>
            <div class="card">Task 3</div>
        </div>

        <!-- Done List -->
        <div class="list">
            <div class="list-title">Done</div>
            <div class="card">Task 4</div>
        </div>
    </div>

    <?php                    
            try {
                if(DB::connection()->getPdo())
                {
                    echo "Successfully connected to the database => "
                                  .DB::connection()->getDatabaseName();
                }
            }
            catch (Exception $e) {
                echo "Unable to connect";
            }
        ?>

        <!-- displaying projects in database as list for testing has to be moved into the cards -->

        <table>
            <tr>
            <td>projectID</td>
            <td>userIdFk</td>
            <td>taskIdFk</td>
            <td>projectName</td>
            <td>projectDescription </td>
            </tr>
            @foreach ($projects as $projects)
                <tr>
                <td>{{ $projects->projectID }}</td>
                <td>{{ $projects->userIdFk }}</td>
                <td>{{ $projects->taskIdFk }}</td>
                <td>{{ $projects->projectName }}</td>
                <td>{{ $projects->projectDescription }}</td>
                </tr>
            @endforeach
        </table>

    <!-- Dragula JS and its initialization -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/dragula/3.7.2/dragula.min.js"></script>
    <script>
        // Make all the lists (containers) a part of dragula drag and drop functionality
        dragula([].slice.call(document.querySelectorAll(".list")), {
            moves: function (el, source, handle, sibling) {
                return true; // allow all items to be draggable
            }
        });
    </script>
</body>

</html>
