import { LessonData } from "@/types/lessonContent";

export const sampleLessonData: LessonData = {
  id: "691152664477569032",
  title: "Popular DBMS Examples (e.g., MySQL, PostgreSQL, Oracle, SQL Server)",
  content: [
    {
      type: "heading",
      content: "Lesson 2.2: Popular DBMS Examples",
    },
    {
      type: "text",
      content:
        "Welcome to Lesson 2.2 of our 'Introducing DBMS' module! In the previous lesson, we learned what a DBMS is and its fundamental role. Now, it's time to get acquainted with the real players in the database world. Just like there are many different car brands, there are various DBMS products, each with its own strengths, weaknesses, and typical use cases.",
    },
    {
      type: "text",
      content:
        "Understanding the landscape of popular DBMS examples is crucial for anyone working with data. It helps you appreciate the diversity, make informed decisions when choosing a database for a project, and recognize the technologies powering countless applications around us.",
    },
    {
      type: "heading",
      content: "1. MySQL: The Ubiquitous Web Database",
    },
    {
      type: "text",
      content:
        "MySQL is perhaps one of the most widely used open-source relational database management systems. Known for its speed, reliability, and ease of use, it has become the backbone for countless web applications and services.",
    },
    {
      type: "heading",
      content: "Key Features:",
    },
    {
      type: "list",
      content: [
        "**Open-Source:** Free to use under the GNU General Public License (GPL), with commercial licenses also available.",
        "**High Performance:** Optimized for fast data retrieval and transaction processing.",
        "**Scalability:** Supports various scaling techniques like replication and clustering.",
        "**Ease of Use:** Relatively straightforward to set up, manage, and integrate with applications.",
        "**Wide Compatibility:** Works on almost all operating systems and integrates well with many programming languages.",
        "**Transactional Support:** Uses storage engines like InnoDB to provide ACID compliance.",
      ],
    },
    {
      type: "code",
      content: {
        language: "sql",
        code: `-- Example: Creating a simple table in MySQL
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert a record
INSERT INTO users (username, email)
VALUES ('john_doe', 'john@example.com');

-- Query the table
SELECT * FROM users WHERE username = 'john_doe';`,
      },
    },
    {
      type: "heading",
      content: "Popular Use Cases:",
    },
    {
      type: "list",
      content: [
        "**Web Applications:** The 'M' in the popular LAMP stack.",
        "**Content Management Systems (CMS):** Powers WordPress, Drupal, and Joomla.",
        "**E-commerce:** Used by platforms such as Magento.",
        "**Social Media:** Popular for many social applications.",
      ],
    },
    {
      type: "heading",
      content: "2. PostgreSQL: The Advanced Open-Source Powerhouse",
    },
    {
      type: "text",
      content:
        "Often dubbed 'the world's most advanced open-source relational database,' PostgreSQL is renowned for its strong adherence to SQL standards, robustness, and extensibility. It's an enterprise-grade database that often competes with commercial offerings.",
    },
    {
      type: "heading",
      content: "Key Features:",
    },
    {
      type: "list",
      content: [
        "**Robust & Feature-Rich:** Offers advanced data types, complex queries, and sophisticated optimization.",
        "**SQL Standards Compliance:** Highly compliant with SQL standards.",
        "**Extensibility:** Allows users to define custom data types, functions, and operators.",
        "**ACID Compliance:** Provides full ACID properties for reliable transaction processing.",
        "**JSON/JSONB Support:** Native support for storing and querying JSON data.",
      ],
    },
    {
      type: "youtube",
      content: {
        url: "https://www.youtube.com/watch?v=qw--VYLpxG4",
        title: "What is PostgreSQL? Introduction to PostgreSQL",
      },
    },
    {
      type: "heading",
      content: "Quick Comparison Table",
    },
    {
      type: "table",
      content: {
        headers: ["DBMS", "Licensing Model", "Primary Strengths", "Typical Use Cases"],
        rows: [
          [
            "**MySQL**",
            "Open-Source (GPL/Commercial)",
            "Ease of use, speed, web applications",
            "Web apps, CMS, E-commerce",
          ],
          [
            "**PostgreSQL**",
            "Open-Source (PostgreSQL License)",
            "Robustness, extensibility, SQL compliance",
            "Complex web apps, GIS, financial services",
          ],
          [
            "**Oracle Database**",
            "Proprietary (Commercial)",
            "Extreme scalability, high availability",
            "Mission-critical enterprise apps",
          ],
          [
            "**SQL Server**",
            "Proprietary (Commercial)",
            "Microsoft ecosystem, BI capabilities",
            "Windows enterprise apps, BI solutions",
          ],
        ],
      },
    },
    {
      type: "quiz",
      content: {
        question:
          "Which of the following DBMS is open-source and known for its strong SQL standards compliance and extensibility?",
        options: ["MySQL", "PostgreSQL", "Oracle Database", "Microsoft SQL Server"],
        correctIndex: 1,
        explanation:
          "PostgreSQL is renowned for its strong SQL standards compliance, extensibility, and is fully open-source under the PostgreSQL License.",
      },
    },
    {
      type: "quiz",
      content: {
        question: "What does ACID stand for in the context of database transactions?",
        options: [
          "Automated, Consistent, Isolated, Durable",
          "Atomicity, Consistency, Isolation, Durability",
          "Atomic, Complete, Integrated, Distributed",
          "Advanced, Concurrent, Independent, Dependable",
        ],
        correctIndex: 1,
        explanation:
          "ACID stands for **Atomicity, Consistency, Isolation, Durability** — the four key properties that guarantee database transactions are processed reliably.",
      },
    },
    {
      type: "heading",
      content: "Conclusion",
    },
    {
      type: "text",
      content:
        "As you can see, the world of DBMS offers a rich variety of options. Each of these popular systems has carved out its niche by excelling in different areas and catering to specific needs. Choosing the 'best' DBMS isn't about finding a single winner, but rather selecting the one that best fits your project's specific requirements, budget, team expertise, and existing infrastructure.",
    },
    {
      type: "text",
      content:
        "This overview provides a foundational understanding of the major players. In the upcoming lessons, we will delve deeper into the core concepts that all these relational DBMS share, such as data modeling, SQL, and database design. Get ready to build on this knowledge!",
    },
  ],
  enriched: true,
};
