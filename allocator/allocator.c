#include "allocator.h"

#include <stdbool.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>

/**
 * Default buffer size if mem_init was not called manually
 */
static const size_t default_buffer_size = 0x10000; // 65536 bytes

/**
 * Pointer to buffer start
 */
static void* baseptr = NULL;

/**
 * Size of OS-allocated buffer
 */
static size_t buffer_size = 0;

/**
 * Data alignment is architecture-dependent
 * and equals to pointer size
 */
static const size_t alignment = sizeof(void*);

/**
 A struct which represents allocated memory buffers
 */
typedef struct AllocatedMemoryNode {
    /**
     * Pointer to next allocated buffer
     */
    struct AllocatedMemoryNode* next;
    /**
     * Size of this buffer
     */
    size_t size;
} AllocatedMemoryNode;

static const size_t node_size = sizeof(AllocatedMemoryNode);
static AllocatedMemoryNode* head = NULL;


/**
 * Free memory buffer from OS when allocator is not needed anymore
 */
static void mem_release();

/**
 * Print @value in hex
 * @param value Value to be printed
 */
static void print_byte(char value);

/**
 * Convert @value from integer representation to hex character
 * @param value
 * @return hex character representation
 */
static char value_to_hex_character(char value);

/**
 * Align size for faster memory access
 * Alignment is stored in @alignment global variable
 * @param size Original size
 * @return Architecture-dependent aligned size
 */
static size_t align_size(size_t size);

void* mem_alloc(const size_t size) {
    if (!buffer_size) {
        if (!mem_init(default_buffer_size)) {
            return NULL;
        }
    }
    const size_t real_size = align_size(size) + node_size;
    const void* free_block_end = baseptr + buffer_size;
    AllocatedMemoryNode* prev_node = NULL;
    AllocatedMemoryNode* cur_node = head;
    while (cur_node) {
        void* const free_block_start = (void*)cur_node + cur_node->size;
        const size_t block_size = free_block_end - free_block_start;
        if (block_size >= real_size) {
            AllocatedMemoryNode* const new_node = free_block_start;
            new_node->next = cur_node;
            new_node->size = real_size;
            if (prev_node) {
                prev_node->next = new_node;
            } else {
                head = new_node;
            }
            return (void*)new_node + node_size;
        }
        free_block_end = cur_node;
        prev_node = cur_node;
        cur_node = cur_node->next;
    }
    return NULL;
}

void* mem_realloc(void* old_addr, size_t new_size) {
    if (old_addr == NULL) return mem_alloc(new_size);
    AllocatedMemoryNode* node = old_addr - node_size;
    AllocatedMemoryNode* prev_node = NULL;
    AllocatedMemoryNode* cur_node = head;
    while (cur_node != NULL && cur_node != node) {
        prev_node = cur_node;
        cur_node = cur_node->next;
    }
    const size_t block_size = (void*) prev_node - ((void*)cur_node + node_size);
    if (block_size >= align_size(new_size)) {
        node->size = align_size(new_size);
        return old_addr;
    }

    void* new_addr = mem_alloc(new_size);
    mem_copy(new_addr, old_addr, node->size - node_size);
    mem_free(old_addr);
    return new_addr;
}

void mem_free(void* addr) {
    AllocatedMemoryNode* node = addr - node_size;
    AllocatedMemoryNode* prev_node = NULL;
    AllocatedMemoryNode* cur_node = head;
    while (cur_node != NULL && cur_node != node) {
        prev_node = cur_node;
        cur_node = cur_node->next;
    }
    if (cur_node == NULL) return;
    if (prev_node) {
        prev_node->next = cur_node->next;
    } else {
        head = cur_node->next;
    }
}

void mem_dump(const char * addr, const size_t size) {
    const char* const end_addr = addr + size;
    size_t printed = 0;
    while (addr < end_addr) {
        print_byte(*addr++);
        printf("%c", (++printed % 24 ? ' ' : '\n'));
    }
}

bool mem_init(const size_t size) {
    // Pretend to be an OS and to have all memory available...
    // Yup, you could use malloc directly. And better just do that.
    // This is just an university assignment
    if (baseptr) mem_release();
    baseptr = malloc(size);
    if (baseptr != NULL) {
        printf("Init with baseptr = %p\n", baseptr);
        buffer_size = size;
        // make sentinel node
        head = baseptr;
        head->size = node_size;
        head->next = NULL;
        return true;
    }
    return false;
}

static void mem_release() {
    free(baseptr);
    baseptr = NULL;
    buffer_size = 0;
    head = NULL;
}

void mem_copy(void* to_void, const void* from_void, const size_t bytes) {
    // Duff's copying device
    const char* from = from_void;
    char* to = to_void;
    size_t n = (bytes + 7) / 8;;
    switch (bytes % 8) {
    case 0: do {    *to = *from++;
    case 7:         *to = *from++;
    case 6:         *to = *from++;
    case 5:         *to = *from++;
    case 4:         *to = *from++;
    case 3:         *to = *from++;
    case 2:         *to = *from++;
    case 1:         *to = *from++;
            } while(--n > 0);
    }
}

static void print_byte(char value) {
    char ah = value / 16;
    char al = value % 16;
    printf("%c%c", value_to_hex_character(ah), value_to_hex_character(al));
}

static char value_to_hex_character(char value) {
    return value + (value < 10 ? '0' : 'A');
}

static size_t align_size(size_t size) {
    return size + ((alignment - size % alignment) % alignment);
}
