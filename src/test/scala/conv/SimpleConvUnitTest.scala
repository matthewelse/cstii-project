// See LICENSE for license details.
package conv

import chisel3.iotesters
import chisel3.iotesters.{PeekPokeTester, ChiselFlatSpec, Driver}

class SimpleConvolutionUnitTester(conv: SimpleConvolution) extends PeekPokeTester(conv) {
  // first test: identity
  poke(conv.io.set_weights, true)
  poke(conv.io.pixel_in.valid, true)
  poke(conv.io.pixel_in.bits, 0)

  step(4)

  poke(conv.io.pixel_in.bits, 1)

  step(1)

  poke(conv.io.pixel_in.bits, 0)
  poke(conv.io.pixel_in.valid, false)
  poke(conv.io.set_weights, false)

  step(1)

  poke(conv.io.pixel_in.valid, true)

  var last_read = 0

  for (i <- 1 to 40) {
    poke(conv.io.pixel_in.bits, i)
    step(1)

    if (peek(conv.io.pixel_out.valid) != 0) {
      if (last_read == 0) {
        last_read = peek(conv.io.pixel_out.bits).toInt
      } else {
        expect(conv.io.pixel_out.bits, last_read + 1)
        last_read = last_read + 1
      }
    }
  }
}

class SimpleConvTester extends ChiselFlatSpec {
  "Basic test using Driver.execute" should "pass" in {
    iotesters.Driver.execute(Array(), () => new SimpleConvolution) {
      c => new SimpleConvolutionUnitTester(c)
    } should be (true)
  }
}

