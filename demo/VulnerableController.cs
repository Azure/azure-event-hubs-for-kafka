// ============================================================
// DEMO ONLY — Vulnerable Controller for Code Scanning Demo
// These vulnerabilities are INTENTIONAL to show CodeQL alerts
// and PR annotations with Copilot Autofix suggestions.
// ============================================================

using Microsoft.AspNetCore.Mvc;
using System.Data.SqlClient;
using System.IO;

namespace ContosoUniversity.API.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class VulnerableController : ControllerBase
    {
        private readonly string _connectionString;

        public VulnerableController(IConfiguration config)
        {
            _connectionString = config.GetConnectionString("DefaultConnection");
        }

        // -------------------------------------------------------
        // VULNERABILITY 1: SQL Injection
        // CodeQL will flag string concatenation in SQL queries.
        // CWE-89 — Improper Neutralization of SQL input
        // -------------------------------------------------------
        [HttpGet("students")]
        public IActionResult SearchStudents(string name)
        {
            var results = new List<string>();

            using var connection = new SqlConnection(_connectionString);
            connection.Open();

            // BAD: User input concatenated directly into SQL query
            string query = "SELECT * FROM Students WHERE LastName = '" + name + "'";
            using var command = new SqlCommand(query, connection);
            using var reader = command.ExecuteReader();

            while (reader.Read())
            {
                results.Add(reader["LastName"].ToString());
            }

            return Ok(results);
        }

        // -------------------------------------------------------
        // VULNERABILITY 2: Cross-Site Scripting (XSS)
        // CodeQL will flag unsanitized user input in HTML response.
        // CWE-79 — Improper Neutralization of Input During Web Page Generation
        // -------------------------------------------------------
        [HttpGet("greeting")]
        public IActionResult Greeting(string username)
        {
            // BAD: User input written directly into HTML without encoding
            return Content("<html><body><h1>Hello, " + username + "!</h1></body></html>", "text/html");
        }

        // -------------------------------------------------------
        // VULNERABILITY 3: Path Traversal
        // CodeQL will flag unsanitized file path from user input.
        // CWE-22 — Improper Limitation of a Pathname to a Restricted Directory
        // -------------------------------------------------------
        [HttpGet("download")]
        public IActionResult DownloadFile(string fileName)
        {
            // BAD: User input used directly in file path without validation
            var filePath = Path.Combine("/app/uploads", fileName);
            var fileBytes = System.IO.File.ReadAllBytes(filePath);
            return File(fileBytes, "application/octet-stream", fileName);
        }
    }
}
