// Generated from dict-work/genmodels.py and the checked model manifest. Do not edit by hand.
package com.irondonkey.dictation;

import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig;

import java.io.File;

/** The model catalog: what each model is, its files, and how sherpa-onnx loads it. */
public final class Models {
    private Models() {}

    public enum Type { TRANSDUCER, CANARY, WHISPER, QWEN3, MOONSHINE }

    public static final class FileInfo {
        public final String path;
        public final long size;
        public final String sha256;
        public final String url;
        FileInfo(String path, long size, String sha256, String url) {
            this.path = path; this.size = size; this.sha256 = sha256; this.url = url;
        }
    }

    public static final class ModelInfo {
        public final String id;
        public final Type type;
        public final String name;
        public final String languages;
        public final String description;
        public final String speed;
        public final String license;
        /** null when the model has no language choice; otherwise {code, label} pairs, Auto first if supported. */
        public final String[][] languageChoices;
        public final long totalBytes;
        public final String repo;
        public final FileInfo[] files;

        ModelInfo(String id, Type type, String name, String languages, String description, String speed,
                  String license, String[][] languageChoices, long totalBytes, String repo, FileInfo[] files) {
            this.id = id; this.type = type; this.name = name; this.languages = languages;
            this.description = description; this.speed = speed; this.license = license;
            this.languageChoices = languageChoices; this.totalBytes = totalBytes; this.repo = repo; this.files = files;
        }

        public String defaultLanguage() {
            return languageChoices == null ? "" : languageChoices[0][0];
        }

        public String languageLabel(String code) {
            if (languageChoices == null) return languages;
            for (String[] c : languageChoices) if (c[0].equals(code)) return c[1];
            return languageChoices[0][1];
        }

        public boolean isUrdu(String code) {
            return type == Type.WHISPER && "ur".equals(code);
        }

        /** Builds the recognizer config for files stored in dir. */
        public OfflineRecognizerConfig config(File dir, String language, int threads) {
            return config(dir, language, threads, "transcribe");
        }

        /** task is "transcribe", or "translate" for Whisper's built-in translation into English. */
        public OfflineRecognizerConfig config(File dir, String language, int threads, String task) {
            OfflineRecognizerConfig c = new OfflineRecognizerConfig();
            c.modelConfig.numThreads = threads;
            c.modelConfig.provider = "cpu";
            c.decodingMethod = "greedy_search";
            c.featConfig.sampleRate = 16000;
            switch (type) {
                case TRANSDUCER:
                    c.featConfig.featureDim = 128;
                    c.modelConfig.modelType = "nemo_transducer";
                    c.modelConfig.transducer.encoder = f(dir, 0);
                    c.modelConfig.transducer.decoder = f(dir, 1);
                    c.modelConfig.transducer.joiner = f(dir, 2);
                    c.modelConfig.tokens = f(dir, 3);
                    break;
                case CANARY:
                    c.featConfig.featureDim = 128;
                    c.modelConfig.canary.encoder = f(dir, 0);
                    c.modelConfig.canary.decoder = f(dir, 1);
                    c.modelConfig.canary.srcLang = language.isEmpty() ? "en" : language;
                    c.modelConfig.canary.tgtLang = c.modelConfig.canary.srcLang;
                    c.modelConfig.canary.usePnc = true;
                    c.modelConfig.tokens = f(dir, 2);
                    break;
                case WHISPER:
                    c.featConfig.featureDim = 80;
                    c.modelConfig.whisper.encoder = f(dir, 0);
                    c.modelConfig.whisper.decoder = f(dir, 1);
                    c.modelConfig.whisper.language = languageChoices == null ? "en" : language;
                    c.modelConfig.whisper.task = languageChoices == null ? "transcribe" : task;
                    c.modelConfig.tokens = f(dir, 2);
                    break;
                case QWEN3:
                    c.featConfig.featureDim = 128;
                    c.modelConfig.qwen3Asr.convFrontend = f(dir, 0);
                    c.modelConfig.qwen3Asr.encoder = f(dir, 1);
                    c.modelConfig.qwen3Asr.decoder = f(dir, 2);
                    c.modelConfig.qwen3Asr.tokenizer = new File(dir, "tokenizer").getAbsolutePath();
                    c.modelConfig.qwen3Asr.maxNewTokens = 256;
                    break;
                case MOONSHINE:
                    c.modelConfig.moonshine.encoder = f(dir, 0);
                    c.modelConfig.moonshine.mergedDecoder = f(dir, 1);
                    c.modelConfig.tokens = f(dir, 2);
                    break;
            }
            return c;
        }

        private String f(File dir, int i) {
            return new File(dir, files[i].path).getAbsolutePath();
        }
    }

    public static final ModelInfo[] ALL = {
        new ModelInfo("parakeet-v2", Type.TRANSDUCER, "Parakeet v2", "English", "Best for English. Most accurate in testing and handles background noise well.", "About 3 seconds per minute of speech on a fast computer", "NVIDIA parakeet-tdt-0.6b-v2, CC BY 4.0", null, 661288526L, "csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8", new FileInfo[]{new FileInfo("encoder.int8.onnx",652282309L,"92a10bd9c23936ea0566c29f963bc57c2a5df4261b6a8921c39abea66557ca8b","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/84b478d541cdbf0c296a7ab5f1e20a6f8f998114/encoder.int8.onnx"),new FileInfo("decoder.int8.onnx",7257753L,"4d097bae3bac5c96018d5293ba39f1b657296102b142affda40378f38b020f41","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/84b478d541cdbf0c296a7ab5f1e20a6f8f998114/decoder.int8.onnx"),new FileInfo("joiner.int8.onnx",1739080L,"77a29f3e5ac7d37074e0af465391b80f314015d5b97f0f90c6c7767f1d56f428","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/84b478d541cdbf0c296a7ab5f1e20a6f8f998114/joiner.int8.onnx"),new FileInfo("tokens.txt",9384L,"ec182b70dd42113aff6c5372c75cac58c952443eb22322f57bbd7f53977d497d","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8/resolve/84b478d541cdbf0c296a7ab5f1e20a6f8f998114/tokens.txt")}),
        new ModelInfo("parakeet-ultra", Type.TRANSDUCER, "Parakeet Ultra", "25 European languages, detected automatically", "For European languages. About as accurate and fast as Parakeet v2.", "About 3 seconds per minute of speech on a fast computer", "Moondream, based on NVIDIA parakeet-tdt-0.6b-v3, CC BY 4.0", null, 670576835L, "csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8", new FileInfo[]{new FileInfo("encoder.int8.onnx",652282345L,"b43072e1b721f18a3df03fcfeb763ff7c4debdf29843598a3aba2f703e2780a3","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8/resolve/402e1190ce85b9793118ee223db21fb0d12f52db/encoder.int8.onnx"),new FileInfo("decoder.int8.onnx",11845274L,"1fab98fe6c12aded87d2da66272cc9e148d0a0044ce3850a12fe56302ec4a922","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8/resolve/402e1190ce85b9793118ee223db21fb0d12f52db/decoder.int8.onnx"),new FileInfo("joiner.int8.onnx",6355277L,"8a71aaccfdba3d451775507a889afd2257d2e754463fd81095a8c8f290f924c2","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8/resolve/402e1190ce85b9793118ee223db21fb0d12f52db/joiner.int8.onnx"),new FileInfo("tokens.txt",93939L,"d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8/resolve/402e1190ce85b9793118ee223db21fb0d12f52db/tokens.txt")}),
        new ModelInfo("canary-180m", Type.CANARY, "Canary 180M Flash", "English, Spanish, German, French", "Light but accurate. Small download and loads quickly.", "About 6 seconds per minute of speech on a fast computer", "NVIDIA canary-180m-flash, CC BY 4.0", new String[][]{{"en","English"},{"es","Spanish"},{"de","German"},{"fr","French"}}, 207170046L, "csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8", new FileInfo[]{new FileInfo("encoder.int8.onnx",132678643L,"7a75b4e2a5857a6dcc0819503bbe3fad66943db4a3ccf21d3f27c633667d303f","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8/resolve/9077164e0d3dd1d5353743e89ceaa1d3a770838c/encoder.int8.onnx"),new FileInfo("decoder.int8.onnx",74437848L,"e41a2ab9c0c2fe81a1e8ade5a45fb02a74bc4db7d1f91b89a54a25e2cf79cba2","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8/resolve/9077164e0d3dd1d5353743e89ceaa1d3a770838c/decoder.int8.onnx"),new FileInfo("tokens.txt",53555L,"2dae6fc7815f9640645e0c765522b278ee0cef49b482d91f6913e334628d3e77","https://huggingface.co/csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8/resolve/9077164e0d3dd1d5353743e89ceaa1d3a770838c/tokens.txt")}),
        new ModelInfo("whisper-turbo", Type.WHISPER, "Whisper Turbo", "99 languages", "Very accurate and knows the most languages, but slow.", "About 23 seconds per minute of speech on a fast computer", "OpenAI Whisper large-v3-turbo, MIT", new String[][]{{"","Auto (detect)"},{"af","Afrikaans"},{"sq","Albanian"},{"am","Amharic"},{"ar","Arabic"},{"hy","Armenian"},{"as","Assamese"},{"az","Azerbaijani"},{"ba","Bashkir"},{"eu","Basque"},{"be","Belarusian"},{"bn","Bengali"},{"bs","Bosnian"},{"br","Breton"},{"bg","Bulgarian"},{"my","Burmese"},{"yue","Cantonese"},{"ca","Catalan"},{"zh","Chinese"},{"hr","Croatian"},{"cs","Czech"},{"da","Danish"},{"nl","Dutch"},{"en","English"},{"et","Estonian"},{"fo","Faroese"},{"fi","Finnish"},{"fr","French"},{"gl","Galician"},{"ka","Georgian"},{"de","German"},{"el","Greek"},{"gu","Gujarati"},{"ht","Haitian Creole"},{"ha","Hausa"},{"haw","Hawaiian"},{"he","Hebrew"},{"hi","Hindi"},{"hu","Hungarian"},{"is","Icelandic"},{"id","Indonesian"},{"it","Italian"},{"ja","Japanese"},{"jw","Javanese"},{"kn","Kannada"},{"kk","Kazakh"},{"km","Khmer"},{"ko","Korean"},{"lo","Lao"},{"la","Latin"},{"lv","Latvian"},{"ln","Lingala"},{"lt","Lithuanian"},{"lb","Luxembourgish"},{"mk","Macedonian"},{"mg","Malagasy"},{"ms","Malay"},{"ml","Malayalam"},{"mt","Maltese"},{"mi","Maori"},{"mr","Marathi"},{"mn","Mongolian"},{"ne","Nepali"},{"no","Norwegian"},{"nn","Norwegian Nynorsk"},{"oc","Occitan"},{"ps","Pashto"},{"fa","Persian"},{"pl","Polish"},{"pt","Portuguese"},{"pa","Punjabi"},{"ro","Romanian"},{"ru","Russian"},{"sa","Sanskrit"},{"sr","Serbian"},{"sn","Shona"},{"sd","Sindhi"},{"si","Sinhala"},{"sk","Slovak"},{"sl","Slovenian"},{"so","Somali"},{"es","Spanish"},{"su","Sundanese"},{"sw","Swahili"},{"sv","Swedish"},{"tl","Tagalog"},{"tg","Tajik"},{"ta","Tamil"},{"tt","Tatar"},{"te","Telugu"},{"th","Thai"},{"bo","Tibetan"},{"tr","Turkish"},{"tk","Turkmen"},{"uk","Ukrainian"},{"ur","Urdu (experimental)"},{"uz","Uzbek"},{"vi","Vietnamese"},{"cy","Welsh"},{"yi","Yiddish"},{"yo","Yoruba"}}, 1036613791L, "csukuangfj/sherpa-onnx-whisper-turbo", new FileInfo[]{new FileInfo("turbo-encoder.int8.onnx",674716297L,"b02dcdf54f348741e93fe732b67d933c8dcb6735655f710640143081db38878b","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-turbo/resolve/2ca6ff69fc878651b770880507669577ac41c2ff/turbo-encoder.int8.onnx"),new FileInfo("turbo-decoder.int8.onnx",361080764L,"20accd02388482eb3a46bd615631adfdc85e1eb2c7db9ea3f02a40ffe6b81547","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-turbo/resolve/2ca6ff69fc878651b770880507669577ac41c2ff/turbo-decoder.int8.onnx"),new FileInfo("turbo-tokens.txt",816730L,"b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-turbo/resolve/2ca6ff69fc878651b770880507669577ac41c2ff/turbo-tokens.txt")}),
        new ModelInfo("whisper-small", Type.WHISPER, "Whisper Small", "99 languages", "Many languages in a smaller download.", "About 16 seconds per minute of speech on a fast computer", "OpenAI Whisper small, MIT", new String[][]{{"","Auto (detect)"},{"af","Afrikaans"},{"sq","Albanian"},{"am","Amharic"},{"ar","Arabic"},{"hy","Armenian"},{"as","Assamese"},{"az","Azerbaijani"},{"ba","Bashkir"},{"eu","Basque"},{"be","Belarusian"},{"bn","Bengali"},{"bs","Bosnian"},{"br","Breton"},{"bg","Bulgarian"},{"my","Burmese"},{"ca","Catalan"},{"zh","Chinese"},{"hr","Croatian"},{"cs","Czech"},{"da","Danish"},{"nl","Dutch"},{"en","English"},{"et","Estonian"},{"fo","Faroese"},{"fi","Finnish"},{"fr","French"},{"gl","Galician"},{"ka","Georgian"},{"de","German"},{"el","Greek"},{"gu","Gujarati"},{"ht","Haitian Creole"},{"ha","Hausa"},{"haw","Hawaiian"},{"he","Hebrew"},{"hi","Hindi"},{"hu","Hungarian"},{"is","Icelandic"},{"id","Indonesian"},{"it","Italian"},{"ja","Japanese"},{"jw","Javanese"},{"kn","Kannada"},{"kk","Kazakh"},{"km","Khmer"},{"ko","Korean"},{"lo","Lao"},{"la","Latin"},{"lv","Latvian"},{"ln","Lingala"},{"lt","Lithuanian"},{"lb","Luxembourgish"},{"mk","Macedonian"},{"mg","Malagasy"},{"ms","Malay"},{"ml","Malayalam"},{"mt","Maltese"},{"mi","Maori"},{"mr","Marathi"},{"mn","Mongolian"},{"ne","Nepali"},{"no","Norwegian"},{"nn","Norwegian Nynorsk"},{"oc","Occitan"},{"ps","Pashto"},{"fa","Persian"},{"pl","Polish"},{"pt","Portuguese"},{"pa","Punjabi"},{"ro","Romanian"},{"ru","Russian"},{"sa","Sanskrit"},{"sr","Serbian"},{"sn","Shona"},{"sd","Sindhi"},{"si","Sinhala"},{"sk","Slovak"},{"sl","Slovenian"},{"so","Somali"},{"es","Spanish"},{"su","Sundanese"},{"sw","Swahili"},{"sv","Swedish"},{"tl","Tagalog"},{"tg","Tajik"},{"ta","Tamil"},{"tt","Tatar"},{"te","Telugu"},{"th","Thai"},{"bo","Tibetan"},{"tr","Turkish"},{"tk","Turkmen"},{"uk","Ukrainian"},{"ur","Urdu (experimental)"},{"uz","Uzbek"},{"vi","Vietnamese"},{"cy","Welsh"},{"yi","Yiddish"},{"yo","Yoruba"}}, 375485327L, "csukuangfj/sherpa-onnx-whisper-small", new FileInfo[]{new FileInfo("small-encoder.int8.onnx",112442483L,"4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/8f3c18b358db4d1f2fc1eae49d75cd20989e4309/small-encoder.int8.onnx"),new FileInfo("small-decoder.int8.onnx",262226114L,"acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/8f3c18b358db4d1f2fc1eae49d75cd20989e4309/small-decoder.int8.onnx"),new FileInfo("small-tokens.txt",816730L,"b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/8f3c18b358db4d1f2fc1eae49d75cd20989e4309/small-tokens.txt")}),
        new ModelInfo("qwen3-asr", Type.QWEN3, "Qwen3-ASR 0.6B", "30 languages, works out the language for you", "Detects the language by itself. Includes Hindi, Arabic and Persian, but not Urdu.", "About 13 seconds per minute of speech on a fast computer", "Alibaba Qwen3-ASR-0.6B, Apache 2.0", new String[][]{{"","Auto (detect)"},{"Arabic","Arabic"},{"Cantonese","Cantonese"},{"Chinese","Chinese"},{"Czech","Czech"},{"Danish","Danish"},{"Dutch","Dutch"},{"English","English"},{"Filipino","Filipino"},{"Finnish","Finnish"},{"French","French"},{"German","German"},{"Greek","Greek"},{"Hindi","Hindi"},{"Hungarian","Hungarian"},{"Indonesian","Indonesian"},{"Italian","Italian"},{"Japanese","Japanese"},{"Korean","Korean"},{"Macedonian","Macedonian"},{"Malay","Malay"},{"Persian","Persian"},{"Polish","Polish"},{"Portuguese","Portuguese"},{"Romanian","Romanian"},{"Russian","Russian"},{"Spanish","Spanish"},{"Swedish","Swedish"},{"Thai","Thai"},{"Turkish","Turkish"},{"Vietnamese","Vietnamese"}}, 987015347L, "csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25", new FileInfo[]{new FileInfo("conv_frontend.onnx",44148281L,"d22dc4423e0940e49884e903d2ea2f7e5567c14fc1aed97e4e26d6b8f208ef9e","https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25/resolve/68818b2313fe77bd06f6a7c5068ff3ef59d02b8a/conv_frontend.onnx"),new FileInfo("encoder.int8.onnx",182491662L,"60748d3e6744a57c9c91e1b17424a6c2990567e8adceb0783940c03ed98fa9d9","https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25/resolve/68818b2313fe77bd06f6a7c5068ff3ef59d02b8a/encoder.int8.onnx"),new FileInfo("decoder.int8.onnx",755914231L,"4f6885be5959ae26af3089d38ee7972c5fafbeeb1cf8d5e76eab6d8b61ca5771","https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25/resolve/68818b2313fe77bd06f6a7c5068ff3ef59d02b8a/decoder.int8.onnx"),new FileInfo("tokenizer/merges.txt",1671853L,"8831e4f1a044471340f7c0a83d7bd71306a5b867e95fd870f74d0c5308a904d5","https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25/resolve/68818b2313fe77bd06f6a7c5068ff3ef59d02b8a/tokenizer/merges.txt"),new FileInfo("tokenizer/tokenizer_config.json",12487L,"4942d005604266809309cabc9f4e9cb89ce855d59b14681fdc0e1cc62ea26c4c","https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25/resolve/68818b2313fe77bd06f6a7c5068ff3ef59d02b8a/tokenizer/tokenizer_config.json"),new FileInfo("tokenizer/vocab.json",2776833L,"ca10d7e9fb3ed18575dd1e277a2579c16d108e32f27439684afa0e10b1440910","https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25/resolve/68818b2313fe77bd06f6a7c5068ff3ef59d02b8a/tokenizer/vocab.json")}),
        new ModelInfo("whisper-base-en", Type.WHISPER, "Whisper Base", "English", "Fast and small. Less accurate with background noise.", "About 5 seconds per minute of speech on a fast computer", "OpenAI Whisper base.en, MIT", null, 160626066L, "csukuangfj/sherpa-onnx-whisper-base.en", new FileInfo[]{new FileInfo("base.en-encoder.int8.onnx",29120534L,"ef6b936f4c9b1d90a3b68634b60c4ed8576b26172b33c2535ec0e933c9edb823","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base.en/resolve/59eea950fc76df2453efb57e6c0fd334548e8ffe/base.en-encoder.int8.onnx"),new FileInfo("base.en-decoder.int8.onnx",130669978L,"f7162ad6db2dbef16cfaeaa7f945b9d7dd9c1b8d472f6aca82f2273d185e4d41","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base.en/resolve/59eea950fc76df2453efb57e6c0fd334548e8ffe/base.en-decoder.int8.onnx"),new FileInfo("base.en-tokens.txt",835554L,"306cd27f03c1a714eca7108e03d66b7dc042abe8c258b44c199a7ed9838dd930","https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base.en/resolve/59eea950fc76df2453efb57e6c0fd334548e8ffe/base.en-tokens.txt")}),
        new ModelInfo("moonshine-tiny", Type.MOONSHINE, "Moonshine Tiny", "English", "Instant, for quick notes. Rough with background noise.", "About 1 second per minute of speech on a fast computer", "Moonshine AI moonshine-tiny, MIT", null, 44243206L, "csukuangfj2/sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27", new FileInfo[]{new FileInfo("encoder_model.ort",13281600L,"94e90a4654fc45cdfedb77c4c08e1739f48862998e58fada384b25118134f221","https://huggingface.co/csukuangfj2/sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27/resolve/d1e6c30921780b8508d04b492dfb3ce8a51605d4/encoder_model.ort"),new FileInfo("decoder_model_merged.ort",30412256L,"cf524c4862d36e9e5ab032eddc73637efd822d70e868ac575cf1a46e1e4708a0","https://huggingface.co/csukuangfj2/sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27/resolve/d1e6c30921780b8508d04b492dfb3ce8a51605d4/decoder_model_merged.ort"),new FileInfo("tokens.txt",549350L,"2870d843e14c1e187bf1913a521562a63b53933814bd7f2145120468f494a049","https://huggingface.co/csukuangfj2/sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27/resolve/d1e6c30921780b8508d04b492dfb3ce8a51605d4/tokens.txt")})
    };

    public static final String DEFAULT_ID = "parakeet-v2";

    public static ModelInfo get(String id) {
        if (id != null) for (ModelInfo m : ALL) if (m.id.equals(id)) return m;
        return null;
    }
}
