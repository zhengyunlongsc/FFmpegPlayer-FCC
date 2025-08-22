#ifndef FFMPEGPLAYER_SAFE_QUEUE_H
#define FFMPEGPLAYER_SAFE_QUEUE_H

#include <queue>  // 引入标准队列
#include <pthread.h>  // 引入 pthread 库

namespace safe_queue {  // 将类放入命名空间，避免与其他内容冲突

    template<typename T>
    class SafeQueue {
        typedef void (*ReleaseCallback)(T *);

        typedef void (*SyncHandle)(std::queue<T> &);

    private:
        bool work;
        std::queue<T> q;

        pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
        pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

        ReleaseCallback release_callback = nullptr;
        SyncHandle sync_handle = nullptr;

    public:
        SafeQueue() {
            pthread_mutex_init(&mutex, nullptr);
            pthread_cond_init(&cond, nullptr);
        }

        ~SafeQueue() {
            clear(0);
            pthread_mutex_destroy(&mutex);
            pthread_cond_destroy(&cond);
        }

        bool empty() {
            pthread_mutex_lock(&mutex);
            bool is_empty = q.empty();
            pthread_mutex_unlock(&mutex);
            return is_empty;
        }

        int size() {
            //pthread_mutex_lock(&mutex);
            int size = q.size();
            //pthread_mutex_unlock(&mutex);
            return size;
        }

        int push(T value) {
            pthread_mutex_lock(&mutex);

            if (value == nullptr) {
                pthread_cond_signal(&cond);
                pthread_mutex_unlock(&mutex);
                return 0;
            }

            if (work) {
                q.push(value);
                pthread_cond_signal(&cond);
                pthread_mutex_unlock(&mutex);
                return 1;
            } else {
                if (value != nullptr && release_callback) {
                    release_callback(&value);
                }
                pthread_mutex_unlock(&mutex);
                return 0;
            }
        }

        int pop(T &value) {
            pthread_mutex_lock(&mutex);
            while (work && q.empty()) {
                pthread_cond_wait(&cond, &mutex);
            }

            if (!q.empty()) {
                value = q.front();
                q.pop();
                pthread_mutex_unlock(&mutex);
                return 1;
            }

            pthread_mutex_unlock(&mutex);
            return 0;
        }

        void clear(int count) {
            pthread_mutex_lock(&mutex);

            int actual_count = count <= 0 ? q.size() : count;
            int current_count = 0;

            while (!q.empty()) {
                T value = q.front();
                if (release_callback) {
                    release_callback(&value);
                }
                q.pop();
                current_count++;
                if (actual_count == current_count) {
                    break;
                }
            }
            pthread_mutex_unlock(&mutex);
        }

        void setWork(bool i) {
            pthread_mutex_lock(&mutex);
            work = i;
            pthread_cond_signal(&cond);
            pthread_mutex_unlock(&mutex);
        }

        void setReleaseCallback(ReleaseCallback callback) {
            this->release_callback = callback;
        }

        void sync() {
            pthread_mutex_lock(&mutex);
            if (sync_handle) {
                sync_handle(q);
            }
            pthread_mutex_unlock(&mutex);
        }

        void setSyncHandle(SyncHandle handle) {
            this->sync_handle = handle;
        }
    };
}

#endif // FFMPEGPLAYER_SAFE_QUEUE_H
