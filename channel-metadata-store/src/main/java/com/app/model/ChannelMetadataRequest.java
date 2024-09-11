package com.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ChannelMetadataRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Country code cannot be blank")
    private String countryCode;

    @NotNull(message = "Metadata cannot be null")
    @Size(min = 1, message = "Metadata must contain at least one channel")
    private List<Channel> metadata;

    @NotBlank(message = "Product cannot be blank")
    private String product;

    @Data
    public static class Channel implements Serializable {
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "Channel name cannot be blank")
        private String name;

        @NotBlank(message = "Language cannot be blank")
        private String language;

        @NotBlank(message = "Type cannot be blank")
        private String type;
    }
}
