package com.ustadmobile.lib.contentscrapers.ck12.plix;

import java.util.Map;

public class PlixLog {

    public Message message;

    public class Message{

        public String method;

        public Params params;

        public class Params{

            public Response response;

            public class Response{

                public String mimeType;

                public String url;

                public Map<String, String> headers;

                public Map<String, String> requestHeaders;

            }

        }

    }

}