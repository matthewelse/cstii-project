package conv

import chisel3.iotesters

object SimpleConvRepl extends App {
  iotesters.Driver.executeFirrtlRepl(args, () => new SimpleConvolution)
}
