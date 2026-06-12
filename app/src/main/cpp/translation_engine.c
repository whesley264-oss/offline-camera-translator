#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "TranslationEngineC"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MAX_WORD_LEN 64

typedef struct {
    char source_word[MAX_WORD_LEN];
    char target_word[MAX_WORD_LEN];
} DictionaryEntry;

void search_binary_dictionary(const char* dict_path, const char* query_word, char* out_translation) {
    FILE* file = fopen(dict_path, "rb");
    if (!file) {
        LOGE("Failed to open dictionary file at path: %s", dict_path);
        strcpy(out_translation, "[Error: Dict IO Fail]");
        return;
    }

    if (fseek(file, 0, SEEK_END) != 0) {
        LOGE("fseek SEEK_END failed");
        strcpy(out_translation, "[Error: Seek Fail]");
        fclose(file);
        return;
    }

    long file_size = ftell(file);
    if (file_size <= 0) {
        strcpy(out_translation, query_word);
        fclose(file);
        return;
    }

    long total_entries = file_size / sizeof(DictionaryEntry);
    long low = 0;
    long high = total_entries - 1;
    DictionaryEntry entry;

    while (low <= high) {
        long mid = low + (high - low) / 2;
        if (fseek(file, mid * sizeof(DictionaryEntry), SEEK_SET) != 0) {
            LOGE("fseek SEEK_SET failed at entry index %ld", mid);
            break;
        }
        
        size_t read_bytes = fread(&entry, sizeof(DictionaryEntry), 1, file);
        if (read_bytes != 1) {
            LOGE("fread structural corruption encountered at index %ld", mid);
            break;
        }

        int cmp = strcmp(query_word, entry.source_word);
        if (cmp == 0) {
            strncpy(out_translation, entry.target_word, MAX_WORD_LEN - 1);
            out_translation[MAX_WORD_LEN - 1] = '\0';
            fclose(file);
            return;
        }
        if (cmp < 0) {
            high = mid - 1;
        } else {
            low = mid + 1;
        }
    }
    
    strncpy(out_translation, query_word, MAX_WORD_LEN - 1);
    out_translation[MAX_WORD_LEN - 1] = '\0';
    fclose(file);
}

JNIEXPORT jstring JNICALL
Java_com_offline_translator_model_NativeTranslationEngine_translateWord(JNIEnv *env, jobject thiz, jstring dict_path, jstring word) {
    if (!dict_path || !word) {
        return (*env)->NewStringUTF(env, "");
    }

    const char *c_dict_path = (*env)->GetStringUTFChars(env, dict_path, 0);
    const char *c_word = (*env)->GetStringUTFChars(env, word, 0);

    char c_translation[MAX_WORD_LEN];
    memset(c_translation, 0, MAX_WORD_LEN);

    search_binary_dictionary(c_dict_path, c_word, c_translation);

    (*env)->ReleaseStringUTFChars(env, dict_path, c_dict_path);

    (*env)->ReleaseStringUTFChars(env, word, c_word);

    return (*env)->NewStringUTF(env, c_translation);
}