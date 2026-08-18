package com.aicourse.controller;

import com.aicourse.model.Lesson;
import com.aicourse.model.LessonMedia;
import com.aicourse.repo.LessonMediaRepo;
import com.aicourse.repo.LessonRepo;
import com.aicourse.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class LessonMediaController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private LessonMediaRepo lessonMediaRepo;

    @Autowired
    private LessonRepo lessonRepo;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lessonId") Long lessonId,
            @RequestParam(value = "source", defaultValue = "upload") String source,
            @RequestParam(value = "alt", required = false) String alt) throws IOException {

        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        String filename = storageService.store(file);
        String url = "/api/media/view/" + filename;

        LessonMedia media = new LessonMedia();
        media.setLesson(lesson);
        media.setUrl(url);
        media.setSource(source);
        media.setAlt(alt);
        lessonMediaRepo.save(media);

        Map<String, Object> response = new HashMap<>();
        response.put("id", media.getId());
        response.put("url", url);
        response.put("filename", filename);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) throws IOException {
        byte[] data = storageService.load(filename);
        String contentType = "image/png"; // simplistic
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
        if (filename.endsWith(".gif")) contentType = "image/gif";
        if (filename.endsWith(".svg")) contentType = "image/svg+xml";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<LessonMedia>> getMediaByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonMediaRepo.findByLessonId(lessonId));
    }
}
