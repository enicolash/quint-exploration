---------------------------- MODULE counterexample ----------------------------

EXTENDS saldo_legacy

(* Constant initialization state *)
ConstInit == TRUE

(* Initial state [_transition(0)] *)
State0 == saldo = 0 /\ totalDebit = 0 /\ totalKredit = 0

(* State1 [_transition(1)] *)
State1 == saldo = -20000 /\ totalDebit = 0 /\ totalKredit = 20000

(* The following formula holds true in the last state and violates the invariant *)
InvariantViolation == saldo < 0

================================================================================
(* Created by Apalache on Tue Nov 17 06:07:55 WIB 2026 *)
(* https://github.com/apalache-mc/apalache *)
