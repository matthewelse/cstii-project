
obj_dir/Vsimple_convolution.mk: conv_tb.cc ../build/simple_convolution.v
	verilator --cc --trace ../build/simple_convolution --exe ./conv_tb.cc -CFLAGS '--std=c++1y'

obj_dir/Vsimple_convolution: obj_dir/Vsimple_convolution.mk
	$(MAKE) -C obj_dir -f Vsimple_convolution.mk -j

tests: obj_dir/Vsimple_convolution
	obj_dir/Vsimple_convolution

# .PHONY(tests)
