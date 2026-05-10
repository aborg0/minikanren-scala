package info.hircus.kanren

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Convenient one-stop import for all miniKanren operations.
  * Usage: import info.hircus.kanren.AllOps._
  * Then use: mod_o(10, 3, x), atom_number("42", x), etc.
  */
@JSExportTopLevel("miniKanrenAllOps")
@JSExportAll
object AllOps {
  // Re-export Prelude operations
  export Prelude.{
    car_o, cdr_o, pair_o, list_o, append_o, member_o, null_o,
    list2pair, pair2list, once, any_o, never_o, always_o
  }

  // Re-export MKMath operations
  export MKMath.{
    add_o, sub_o, mul_o, div_o, mod_o,
    lt_o, gt_o, le_o, ge_o, eq_num_o, ne_num_o
  }

  // Re-export StringOps operations
  export StringOps.{
    atom_concat, atom_codes, atom_chars, atom_length, atom_string,
    sub_atom, upcase_atom, downcase_atom,
    atom_number, number_codes, number_chars
  }

  // Re-export ListOps operations
  export ListOps.{
    length_o, reverse_o, nth0_o, nth1_o, last_o,
    prefix_o, suffix_o, select_o
  }

  // Re-export core MiniKanren
  export MiniKanren.{
    Goal, Subst, make_var, run, succeed, fail,
    all, any_e, if_e, both, mkEqual
  }
}
