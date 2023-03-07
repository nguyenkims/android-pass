package proton.pass.domain

data class VaultWithItemCount(
    val vault: Vault,
    val activeItemCount: Long,
    val trashedItemCount: Long
)