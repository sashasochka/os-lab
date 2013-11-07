#include "allocator.h"

#include <math.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct BlockHeader {
    struct BlockHeader* next_header;
} BlockHeader;

typedef struct DividedPageHeader {
    BlockHeader* next_free_block;
    size_t block_size;
} DividedPageHeader;

typedef struct ClassNode {
    struct ClassNode* next;
    DividedPageHeader* first_block_header;
} ClassNode;

typedef struct BlockClassesNode {
    size_t block_size;
    struct BlockClassesNode* next;
    ClassNode* first_item;
} BlockClassesNode;

typedef struct Page {
} Page;

static void mem_init();
static int find_page_sequence(size_t pages_needed);
static void* create_block_page(size_t block_size);
static void delete_block(DividedPageHeader* page_header, void* addr);
static size_t allign_size(size_t size);
static bool address_out_of_range(void* addr);


// 0x20000000 = 0.5 GiB  0x64000 - 100 pages
static const size_t buffer_size = 0x6400;
static const size_t page_size = 0x1000; // 4096 bytes
static const size_t page_count = buffer_size / page_size;

static BlockClassesNode* first_class = NULL;
static bool is_initialized_memory = false;
static Page* pages[page_count];
static void* memory_start;

void mem_init() {
    if (!is_initialized_memory) {
        is_initialized_memory = true;
        memory_start = (Page*) malloc(buffer_size);
    }
}

void* mem_alloc(size_t size) {
    size_t real_size = allign_size(size);

    if (!is_initialized_memory) {
        mem_init();
    }
    if (memory_start == NULL) {
        return NULL;
    }

    if (real_size <= page_size / 2 - sizeof(DividedPageHeader)) {
        // page is divided into blocks of the same size
        DividedPageHeader* block_header = NULL; // block with a free space is available
        BlockClassesNode* node = first_class;
        BlockClassesNode* prev_node = NULL;

        while (node != NULL) {
            if (node->block_size == real_size) {
                block_header = node->first_item->first_block_header;
                break;
            }
            prev_node = node;
            node = node->next;
        }

        if (node == NULL) { // new class addition
            if (first_class == NULL) { // if no class exists, then create it,
                // add new class Item, which points to a new
                // page header
                first_class = malloc(sizeof(BlockClassesNode));
                ClassNode* first_item = malloc(sizeof(ClassNode));
                first_item->next = NULL;
                first_class->first_item = first_item;
                void* start_address = create_block_page(real_size);

                if (start_address == NULL) {
                    return NULL;    // no pages available
                }

                first_class->block_size = real_size;
                first_class->next = NULL;
                first_class->first_item->first_block_header = (DividedPageHeader*)start_address;
                return (void*)((size_t)start_address + sizeof(DividedPageHeader));
            } else {                                                // at least one class exist,
                // but no class with needed block size
                node = malloc(sizeof(BlockClassesNode));
                ClassNode* first_item = malloc(sizeof(ClassNode));
                first_item->next = NULL;
                node->first_item = first_item;
                prev_node->next = node;
                void* start_address = create_block_page(real_size);

                if (start_address == NULL) {
                    return NULL;    // no pages available
                }

                node->block_size = real_size;
                node->next = NULL;
                node->first_item->first_block_header = (DividedPageHeader*)start_address;
                return (void*)((size_t)start_address + sizeof(DividedPageHeader));
            }
        } else { // class found
            BlockHeader* free_block = block_header->next_free_block;

            if (free_block == NULL) {
                return NULL;
            }

            if (free_block->next_header == NULL) {
                BlockHeader* temp = (BlockHeader*)
                        ((size_t)block_header->next_free_block + real_size);
                block_header->next_free_block = temp;
                temp->next_header = NULL;
            } else {
                block_header->next_free_block = free_block->next_header;
            }

            // no more space in the page after block adding
            if ((size_t)block_header + page_size - (size_t)free_block - real_size < real_size) {
                ClassNode* prev_item = NULL;

                // delete block header from its item in the class
                for (ClassNode* item = node->first_item; item != NULL; item = item->next) {
                    if (item->first_block_header == block_header) {
                        if (prev_item == NULL) {
                            node->first_item = item->next;
                        } else {
                            prev_item->next = item->next;
                        }

                        break;
                    }

                    prev_item = item;
                }

                if (node->first_item == NULL) {
                    if (prev_node == NULL) {
                        first_class = NULL;
                    } else {
                        prev_node->next = node->next;
                    }
                }
            }

            return free_block;
        }
    } else {
        size_t pages_needed = (size_t) ceil((double)real_size / (double)page_size);
        int first_free_page = find_page_sequence(pages_needed);

        if (first_free_page == -1) {
            return NULL;
        }

        void* start_address = (void*)((size_t)memory_start + first_free_page * page_size);

        for (size_t i = first_free_page; i < first_free_page + pages_needed; ++i) {
            pages[i] = (Page*)start_address;
        }

        return start_address;
    }
}

void* mem_realloc(void* addr, size_t size) {
    if (address_out_of_range(addr)) {
        return NULL;
    }

    size_t real_size = allign_size(size);
    void* new_addr = mem_alloc(real_size);

    if (new_addr) {
        mem_copy(new_addr, addr, real_size);
        mem_free(addr);
        return new_addr;
    } else {
        return NULL;
    }
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

void mem_free(void* addr) {
    //address is out of memory bounds
    if (address_out_of_range(addr)) {
        return;
    }

    size_t space_distance = (size_t)addr - (size_t)memory_start;
    int page_index = space_distance / page_size;
    bool is_block_page = true;

    // page is a block page only if address points not to the start of a new page
    if (space_distance % page_size == 0) {
        is_block_page = false;
    }

    if (is_block_page) {
        // delete one block from the (pageIndex+1)-th page
        DividedPageHeader* page_header = (DividedPageHeader*) pages[page_index];
        size_t block_size = page_header->block_size;
        BlockClassesNode* class_node;                                // classNode
        BlockClassesNode* prev_class_node = NULL;

        // try to find a class, where the block belongs to
        for (class_node = first_class; class_node != NULL; class_node = class_node->next) {
            if (class_node->block_size == block_size) {
                break;
            }

            prev_class_node = class_node;
        }

        // no class for the block found, which means that new class needed
        if (class_node == NULL) {
            BlockClassesNode* new_class_node = malloc(sizeof(BlockClassesNode));
            new_class_node->block_size = block_size;
            new_class_node->next = NULL;
            ClassNode* new_class_item = malloc(sizeof(ClassNode));
            new_class_item->first_block_header = page_header;
            new_class_item->next = NULL;
            new_class_node->first_item = new_class_item;

            if (first_class == NULL) {
                // no classes exist
                first_class = new_class_node;
            } else {
                prev_class_node->next = new_class_node;
            }

            // delete the block
            delete_block(page_header, addr);
        } else {
            bool one_block_on_page = false;
            size_t used_space = (size_t)page_header->next_free_block
                               - (size_t)page_header - sizeof(DividedPageHeader);

            for (BlockHeader* b_header = page_header->next_free_block;
                     b_header->next_header != NULL;
                     b_header = b_header->next_header) {
                used_space += (size_t)b_header->next_header - (size_t)b_header;
            }

            if (used_space == block_size) {
                one_block_on_page = true;
            }

            if (one_block_on_page) {
                // only one block in the page
                pages[page_index] = NULL; // mark the page as free
                ClassNode* cur_class_item;
                ClassNode* prev_class_item = NULL;

                // seek for a class item, the block belongs to
                for (cur_class_item = class_node->first_item
                                    ; cur_class_item->next != NULL
                        ; cur_class_item = cur_class_item->next) {
                    if (cur_class_item->first_block_header == page_header) {
                        break;
                    }

                    prev_class_item = cur_class_item;
                }

                // delete class item of the block
                if (prev_class_item) {
                    prev_class_item->next = cur_class_item->next;
                } else {
                    class_node->first_item = NULL;        // class has no items

                    // at least two classes exist
                    if (prev_class_node) {
                        prev_class_node->next = class_node->next;
                    }
                    // only one class exists
                    else {
                        first_class = NULL;
                    }
                }
            } else {
                // delete the block, create new header if needed
                delete_block(page_header, addr);
            }
        }
    } else {
        // delete all pages, given to user as one virtual page
        for (int i = page_index + 1;
                i < page_count && pages[page_index] == pages[i]; ++i) {
            pages[i] = NULL;
        }

        pages[page_index] = NULL;
    }
}

void mem_dump() {
    for (int i = 0; i < page_count; ++i) {
        printf("Page #%d", i + 1);

        if (pages[i] == NULL) {
            printf(": free\n");
        } else {
            bool is_divided = false;

            for (BlockClassesNode* node = first_class; node != NULL; node = node->next) {
                for (ClassNode* item = node->first_item; item != NULL; item = item->next) {
                    // page consists of blocks
                    if ((void*)(item->first_block_header) == pages[i]) {
                        is_divided = true;
                        break;
                    }

                    if (is_divided) {
                        break;
                    }
                }
            }

            if (!is_divided) {
                printf(": full\n");
                continue;
            }

            printf(": divided");
            size_t free_space = 0;
            int block_number = 1;
            DividedPageHeader* header = (DividedPageHeader*)pages[i];
            BlockHeader* b_header;
            BlockHeader* prev_b_header = (BlockHeader*)
                    ((size_t)header + sizeof(DividedPageHeader));
            printf(", block size: %lu\n", (unsigned long) header->block_size);

            for (b_header = header->next_free_block; b_header != NULL;
                    b_header = b_header->next_header) {
                size_t used_space = sizeof(BlockHeader*) * (size_t)(b_header - prev_b_header);

                if (used_space) {
                    if (b_header != header->next_free_block) {
                        used_space -= header->block_size;
                    }

                    int blocks_quant = used_space / header->block_size;

                    for (int i = 0; i < blocks_quant; ++i) {
                        printf("    block #%2d: used\n", block_number);
                        block_number++;
                    }
                } else {
                    free_space += header->block_size;
                }

                if (b_header->next_header != NULL) {
                    printf("    block #%2d: free\n", block_number);
                }

                block_number++;
                prev_b_header = b_header;
            }

            free_space = free_space + (size_t)header + page_size - (size_t)prev_b_header;

            if (free_space != 0) {
                printf("    free space available: %5lu\n", (unsigned long) free_space);
            }
        }
    }

    printf("\n");
}

int find_page_sequence(size_t pages_needed) {
    int free_counter = 0; // quantity of sequential free pages
    int first_free_page_index = 0;

    for (int i = 0; i < page_count; ++i) {
        if (free_counter == pages_needed) {
            break;
        }

        if (pages[i] == NULL) { // page is free
            free_counter++;
        } else { // page is full or used
            free_counter = 0;
            first_free_page_index = i + 1;
        }
    }

    if (free_counter == pages_needed) {
        return first_free_page_index;
    } else {
        return -1;
    }
}

void* create_block_page(size_t block_size) {
    int free_page_index = find_page_sequence(1); // seek for 1 free page

    if (free_page_index == -1) {
        return NULL;
    }

    // calculate address of a new page
    void* start_address = (void*)
        ((size_t)memory_start + free_page_index * page_size);
    pages[free_page_index] = (Page*)start_address; // mark page as used
    // operations with page header
    DividedPageHeader* page_header = (DividedPageHeader*)start_address;
    page_header->block_size = block_size;
    // next free header
    BlockHeader* free_header = (BlockHeader*)
            ((size_t)start_address + sizeof(DividedPageHeader) + block_size);
    page_header->next_free_block = free_header;
    free_header->next_header = NULL;
    return start_address;
}

void delete_block(DividedPageHeader* page_header, void* addr) {
    size_t block_size = page_header->block_size;
    BlockHeader* b_header;
    BlockHeader* prev_header = NULL;

    // look for header of the previous to current free block
    for (b_header = (BlockHeader*)page_header; (void*)b_header < addr;
            b_header = b_header->next_header) {
        prev_header = b_header;
    }

    // new node for released block of memory
    BlockHeader* new_header = (BlockHeader*)addr;

    // block is before some free block, so delete all free block
    // node and add new to addr instead
    if ((size_t)addr + block_size == (size_t)b_header) {
        new_header->next_header = b_header->next_header;
        prev_header->next_header = new_header;
        // just add new node
    } else if ((size_t)prev_header + block_size != (size_t)addr) {
        new_header->next_header = prev_header->next_header;
        prev_header->next_header = new_header;
    }
}

size_t allign_size(size_t size) {
    return pow(2, ceil(log(size)/log(2)));
}

bool address_out_of_range(void* addr) {
    return addr < memory_start || (size_t)addr > (size_t)memory_start + page_count * page_size;
}