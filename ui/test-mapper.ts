import { normalizeLessonData } from "./src/lib/lessonMapper";

const test1 = { content: "Raw text" };
console.log("test1:", JSON.stringify(normalizeLessonData(test1)));

const test2 = { content: [{ type: "text", text: "Hello" }] };
console.log("test2:", JSON.stringify(normalizeLessonData(test2)));

const test3 = "Just a string";
console.log("test3:", JSON.stringify(normalizeLessonData(test3)));

const test4 = { data: "Just a string" };
console.log("test4:", JSON.stringify(normalizeLessonData(test4)));
