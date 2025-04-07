document.addEventListener("DOMContentLoaded", function () {
    const loginForm = document.getElementById("login-form");
    const registerForm = document.getElementById("register-form");

    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const username = document.getElementById("login-username").value;
            const password = document.getElementById("login-password").value;

            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password })
            });

            const result = await response.text();
            if (response.ok) {
                localStorage.setItem("token", result);
                alert("Login successful!");
                // Redirect after login
                window.location.href = "/dashboard.html";
            } else {
                alert("Login failed: " + result);
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const username = document.getElementById("register-username").value;
            const password = document.getElementById("register-password").value;

            const response = await fetch("/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password })
            });

            const result = await response.text();
            if (response.ok) {
                localStorage.setItem("token", result);
                alert("Registration successful!");
                window.location.href = "/dashboard.html";
            } else {
                alert("Registration failed: " + result);
            }
        });
    }
});
