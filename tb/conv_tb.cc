#define CATCH_CONFIG_RUNNER

#include <iostream>
#include <iomanip>

#include "catch.h"
#include "obj_dir/Vsimple_convolution.h"

void step(Vsimple_convolution *conv) {
    conv->clock = 0;
    conv->eval();
    conv->clock = 1;
    conv->eval();
}

void reset(Vsimple_convolution *conv) {
    conv->reset = 1;
    step(conv);
}

void start(Vsimple_convolution *conv) {
    conv->reset = 0;
}

#define min(x, y) (x < y ? x : y)

void dump_buffer(CData* buffer, size_t size, const size_t rows) {
    std::cout << "[ " << std::endl << "  ";

    size_t offset = 0;

    for (size_t row = 0; row < rows; row++) {
        for (int i = 0; i < min(16, size); i++) {
            std::cout << int(buffer[i + offset]) << " ";
        }

        offset += min(16, size);
        size -= min(16, size);
    }

    std::cout << std::endl;
    std::cout << "]" << std::endl;
}

void dump_grid(uint8_t grid[14][16], size_t w, size_t h) {
    for (int i = 0; i < h; i++) {
        for (int j = 0; j < w; j++) {
            std::cout << std::setfill('0') << std::setw(2);
            std::cout << int(grid[i][j]) << " ";
        }
        std::cout << std::endl;
    }
}

void set_weights(Vsimple_convolution *conv, uint8_t weights[3][3]) {
    conv->io_pixel_in_valid = 1;
    conv->io_set_weights = 1;

    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            conv->io_pixel_in_bits = weights[i][j];
            step(conv);
        }
    }

    // make sure that the weights are defined correctly
    REQUIRE(conv->SimpleConvolution__DOT__weights_0 == weights[0][0]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_1 == weights[0][1]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_2 == weights[0][2]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_3 == weights[1][0]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_4 == weights[1][1]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_5 == weights[1][2]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_6 == weights[2][0]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_7 == weights[2][1]);
    REQUIRE(conv->SimpleConvolution__DOT__weights_8 == weights[2][2]);
}

void convolve(Vsimple_convolution *conv, uint8_t window[16][16], uint8_t weights[3][3], uint8_t out[14][16]) {
    conv->io_pixel_in_valid = 1;

    size_t out_i = 0;
    size_t out_j = 0;

    for (int i = 0; i < 16; i++) {
        for (int j = 0; j < 16; j++) {
            REQUIRE(conv->SimpleConvolution__DOT__buffer_in == (i * 16 + j) % 35);
            conv->io_pixel_in_bits = window[i][j];
            step(conv);

            if (conv->io_pixel_out_valid) {
                //std::cout << "valid: " << int(conv->io_pixel_out_bits) << std::endl;
                out[out_i][out_j] = conv->io_pixel_out_bits;

                out_j++;

                if (out_j == 16) {
                    out_j = 0;
                    out_i++;
                }
            }
        }
    }
}

TEST_CASE("identity convolution", "[identity]") {
    Vsimple_convolution *conv = new Vsimple_convolution;
    reset(conv); 
    start(conv);

    uint8_t weights[3][3] = {0};
    weights[1][1] = 1;

    set_weights(conv, weights);

    conv->io_pixel_in_valid = 0;
    conv->io_set_weights = 0;

    REQUIRE(conv->SimpleConvolution__DOT__buffer_in == 0);

    step(conv);

    uint8_t x[16][16] = {{0}};
    uint8_t y[14][16] = {{0}};

    for (int i = 1; i < 15; i++) {
        for (int j = 1; j < 15; j++) {
            x[i][j] = i;
        }
    }

    conv->io_pixel_in_valid = 1;

    convolve(conv, x, weights, y); 
    dump_grid(y, 14, 14);

    for (int i = 0; i < 14; i++) {
        for (int j = 0; j < 14; j++) {
            REQUIRE(y[i][j] == i + 1);
        }
    }

    delete conv;
}

int main(int argc, const char **argv) {
    Verilated::commandArgs(argc, argv);

    int result = Catch::Session().run(argc, argv);
    return (result < 0xff ? result : 0xff);
}

