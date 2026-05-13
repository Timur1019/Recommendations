package com.kurashnation.dto.response;

import org.springframework.core.io.Resource;

public record MediaDownload(Resource resource, String contentType, String filename) {
}
