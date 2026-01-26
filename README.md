# Arovm

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Docker](https://img.shields.io/badge/docker-supported-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-green)

**Arovm** is a [Briefly describe what the website does, e.g., "comprehensive financial tracking dashboard" or 
"personal portfolio website"] designed to provide [mention the main benefit, e.g., "real-time analytics" or 
"showcase of development projects"].

# Arovm IDE - Full-Stack Cloud-Based Code Editor
**Arovm IDE** is a high-performance, web-based integrated development environment (IDE) built for the modern
developer. It allows users to write, compile, and execute code in multiple languages (Java 21, Python, C++) 
within isolated, secure Docker containers.


## 🚀 Features




## 🚀 Features

* **Multi-Language Execution:** Run Java 21, Python 3, and C/C++ instantly.
* **Real-Time Streaming:** Integrated with **WebSockets** to provide live, line-by-line terminal output—perfect for debugging infinite loops.
* **Secure Sandboxing:** Every execution happens in a fresh Docker container, ensuring the host server remains secure.
* **Profile Management:** Save and manage your projects in a personal workspace backed by **AWS RDS (MySQL)**.
* **Web Preview:** Built-in HTML/CSS/JS previewer for front-end development.
* **Modern UI:** A clean, dark-themed editor powered by the Ace Editor engine.

## 🛠️ Tech Stack

* **Backend:** Java 21, Spring Boot 3.x, Spring Security, Spring WebSocket.
* **Frontend:** Thymeleaf, JavaScript (ES6+), Bootstrap 5, Ace Editor.
* **Infrastructure:** Docker (for code execution), AWS RDS (Database), AWS EC2 (Hosting).
* **Database:** MySQL / Amazon Aurora.



## ⚙️ Prerequisites

Before running this project locally, ensure you have:
1. **Java 21 JDK** installed.
2. **Docker Desktop** installed and running.
3. **Maven** for dependency management.
4. **MySQL** or an active **AWS RDS** instance.

## 🏃 Quick Start

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/your-username/arovm-ide.git](https://github.com/your-username/arovm-ide.git)
   cd arovm-ide
* **User Authentication:** Secure login and registration functionality.
* **Responsive Design:** Fully optimized for desktop, tablet, and mobile devices.
* **Real-time Data:** [If applicable, e.g., "Live tracking of gold/silver prices"].
* **Secure Backend:** Built with Java Spring Boot for robust performance.
* **Containerized:** Docker support for easy deployment and scaling.

## 🛠️ Tech Stack

**Frontend:**
* HTML5 / CSS3
* JavaScript
* [Framework if used, e.g., React, Thymeleaf, or Bootstrap]

**Backend:**
* Java (JDK 17)
* Spring Boot (MVC, Data JPA)
* Hibernate

**Database:**
* [e.g., MySQL / PostgreSQL]

**DevOps & Deployment:**
* Docker & Docker Compose
* AWS EC2
* AWS RDS

## 📋 Prerequisites

Before you begin, ensure you have the following installed:
* [Java JDK 17+(21)](https://www.oracle.com/java/technologies/downloads/)
* [Docker Desktop](https://www.docker.com/products/docker-desktop)
* [Maven](https://maven.apache.org/)

## ⚡ Installation & Local Setup

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/your-username/arovm.git](https://github.com/your-username/arovm.git)
    cd arovm
    ```

2.  **Configure the Database**
    * Open `src/main/resources/application.properties`.
    * Update your database credentials:
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/arovm_db
        spring.datasource.username=root
        spring.datasource.password=your_password
        ```

3.  **Build the Project**
    ```bash
    mvn clean install
    ```

4.  **Run the Application**
    ```bash
    mvn spring-boot:run
    ```
    The app will be available at `http://localhost:8080`.

## 🐳 Running with Docker

If you prefer to run the application in a container (recommended):

1.  **Build the Docker Image**
    ```bash
    docker build -t arovm-app .
    ```

2.  **Run the Container**
    ```bash
    docker run -d -p 80:8080 --name arovm-container arovm-app
    ```
    *Note: Ensure your database is accessible to the container, or use Docker Compose.*

## ☁️ Deployment (AWS)

To deploy this on an AWS EC2 instance:

1.  Push the latest code to GitHub.
2.  SSH into your EC2 instance.
3.  Pull the code and build the Docker image.
4.  Run using:
    ```bash
    docker run -d -p 80:8080 -e DB_HOST=your-rds-endpoint arovm-app
    ```

## 🤝 Contributing

Contributions are welcome!
1.  Fork the project.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

## 📞 Contact

**Vicky Kumar** - [arovm.ltd@zohomail.in]
Project Link: [https://github.com/Vicky0811/arovm]