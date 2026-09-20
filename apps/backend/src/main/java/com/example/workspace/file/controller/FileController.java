package com.example.workspace.file.controller;

import com.example.workspace.common.api.*;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.domain.SourceType;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.file.application.*;
import com.example.workspace.file.dto.FileDtos.*;
import com.example.workspace.file.dto.FileDtos.File;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.infrastructure.storage.*;
import com.example.workspace.tag.controller.TagController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1")
@AuthenticatedApi
@Tag(name="Files",description="私有文件资料库、两阶段上传与任务/笔记附件")
public class FileController {
    private final FileCatalog files;
    private final FileUploadService uploads;
    private final ObjectStorage storage;
    private final IsolatedFileParser parser;
    public FileController(FileCatalog files,FileUploadService uploads,ObjectStorage storage,IsolatedFileParser parser) {this.files=files;this.uploads=uploads;this.storage=storage;this.parser=parser;}
    @GetMapping("/files/capabilities") public Limits capabilities(CurrentUser user) {return files.capabilities(user);}
    @PostMapping("/files/uploads") public ResponseEntity<Upload> create(CurrentUser user,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateUpload body) {
        var result=uploads.create(user,key,body);return ResponseEntity.status(result.created()?201:200).body(result.upload());
    }
    @GetMapping("/files/uploads/{id}") public Upload upload(CurrentUser user,@PathVariable UUID id) {return uploads.get(user,id);}
    @PutMapping(value="/files/uploads/{id}/content",consumes=MediaType.ALL_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="接收原始字节并校验发布",requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(mediaType="application/octet-stream",schema=@Schema(type="string",format="binary"))))
    public ResponseEntity<File> content(CurrentUser user,@PathVariable UUID id,HttpServletRequest request) throws IOException {
        var result=uploads.receive(user,id,request.getInputStream(),request.getContentType());return ResponseEntity.status(result.created()?201:200).body(result.file());
    }
    @GetMapping("/files") public PageResponse<File> list(CurrentUser user,@RequestParam(required=false) String q,@RequestParam(required=false) UUID projectId,
            @RequestParam(required=false) List<UUID> tagIds,@RequestParam(defaultValue="ALL") String tagMode,@RequestParam(required=false) List<String> mediaTypes,
            @RequestParam(required=false) String extractionStatus,@RequestParam(required=false) Instant updatedFrom,@RequestParam(required=false) Instant updatedTo,
            @RequestParam(defaultValue="false") boolean uncategorized,@RequestParam(defaultValue="false") boolean unassigned,
            @RequestParam(defaultValue="false") boolean includeArchived,@RequestParam(defaultValue="active") String view,@Valid @ParameterObject PageQuery page) {
        return files.list(user,q,projectId,tagIds,tagMode,mediaTypes,extractionStatus,updatedFrom,updatedTo,uncategorized,unassigned,includeArchived,view,page);
    }
    @GetMapping("/files/{id}") public File get(CurrentUser user,@PathVariable UUID id) {return files.get(user,id);}
    @PatchMapping("/files/{id}")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=Update.class))))
    public File update(CurrentUser user,@PathVariable UUID id,@RequestBody JsonNode body) {
        if(!body.path("version").isIntegralNumber() || body.path("version").asLong(-1)<0)throw SourceAccess.invalid("请输入版本号");
        if(!body.has("displayName")&&!body.has("projectId"))throw SourceAccess.invalid("请选择要修改的字段");
        if(body.has("displayName")&&!body.get("displayName").isString())throw SourceAccess.invalid("文件名无效");
        UUID project=null;
        if(body.hasNonNull("projectId")) {try{project=UUID.fromString(body.get("projectId").asText());}catch(IllegalArgumentException e){throw SourceAccess.invalid("项目 ID 无效");}}
        return files.update(user,id,body.path("version").asLong(),body.has("displayName")?body.get("displayName").asText():null,body.has("projectId"),project);
    }
    @DeleteMapping("/files/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser user,@PathVariable UUID id,@RequestHeader("If-Match") String version) {files.delete(user,id,TagController.parseVersion(version));}
    @PostMapping("/files/{id}/restore") public File restore(CurrentUser user,@PathVariable UUID id,@Valid @RequestBody Restore body) {return files.restore(user,id,body.version());}
    @GetMapping("/files/{id}/text") public Text text(CurrentUser user,@PathVariable UUID id) {return files.text(user,id);}
    @PostMapping("/files/{id}/extraction-retries") @ResponseStatus(HttpStatus.ACCEPTED)
    public File retryExtraction(CurrentUser user,@PathVariable UUID id) {return files.retryExtraction(user,id);}
    @GetMapping(value="/files/{id}/preview",produces="image/png")
    public ResponseEntity<byte[]> preview(CurrentUser user,@PathVariable UUID id) {
        var file=files.get(user,id);var input=files.parserInput(user,id);
        if(!Set.of("image/png","image/jpeg","image/webp","application/pdf").contains(file.mediaType()))throw SourceAccess.invalid("此格式请使用正文预览");
        var result=parser.parse(input.ref(),input.extension(),input.size(),input.sha256(),true);
        if(!result.status().equals("READY"))throw new com.example.workspace.common.exception.AppException(ErrorCode.FILE_CONTENT_UNAVAILABLE,HttpStatus.SERVICE_UNAVAILABLE,"预览暂不可用，可下载原文件");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").body(result.content());
    }
    @GetMapping(value="/files/{id}/content",produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiResponse(responseCode="200",content=@Content(mediaType="application/octet-stream",schema=@Schema(type="string",format="binary")))
    public void download(CurrentUser user,@PathVariable UUID id,@RequestParam(defaultValue="attachment") String disposition,HttpServletResponse response) throws IOException {
        if(!Set.of("attachment","inline").contains(disposition))throw SourceAccess.invalid("下载方式无效");
        var file=files.get(user,id);var ref=files.contentRef(user,id);
        // Inline raw images and PDFs are deliberately not exposed before preview sanitization.
        boolean inline=disposition.equals("inline") && Set.of("text/plain","text/markdown").contains(file.mediaType());
        try(var content=storage.open(ref)) {
            response.setHeader("Content-Disposition",ContentDisposition.builder(inline?"inline":"attachment").filename(file.displayName(),StandardCharsets.UTF_8).build().toString());
            response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Cache-Control","private, no-store");
            response.setHeader("Content-Security-Policy","sandbox; default-src 'none'");
            response.setContentType(inline?"text/plain;charset=UTF-8":file.mediaType());response.setContentLengthLong(content.metadata().size());
            content.stream().transferTo(response.getOutputStream());
        } catch(StorageException e) {throw FileCatalog.storageFailure(e);}
    }
    @GetMapping("/{resource:tasks|notes}/{id}/files") public PageResponse<File> attachments(CurrentUser user,@PathVariable String resource,@PathVariable UUID id,@Valid @ParameterObject PageQuery page) {return files.attachments(user,SourceType.fromResource(resource),id,page);}
    @PutMapping("/{resource:tasks|notes}/{id}/files/{fileId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attach(CurrentUser user,@PathVariable String resource,@PathVariable UUID id,@PathVariable UUID fileId) {files.attach(user,SourceType.fromResource(resource),id,fileId,false);}
    @DeleteMapping("/{resource:tasks|notes}/{id}/files/{fileId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(CurrentUser user,@PathVariable String resource,@PathVariable UUID id,@PathVariable UUID fileId) {files.attach(user,SourceType.fromResource(resource),id,fileId,true);}
}
