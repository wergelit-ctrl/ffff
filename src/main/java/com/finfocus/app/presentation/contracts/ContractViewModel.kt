package com.finfocus.app.presentation.contracts

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.data.contract.ContractRepository
import com.finfocus.app.domain.model.ContractInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val repository: ContractRepository,
) : ViewModel() {
    private val _contracts = MutableStateFlow<List<ContractInfo>>(emptyList())
    val contracts: StateFlow<List<ContractInfo>> = _contracts

    private val _contractsPath = MutableStateFlow(repository.contractsPath())
    val contractsPath: StateFlow<String> = _contractsPath

    init {
        refresh()
    }

    fun mergeAll() = viewModelScope.launch {
        repository.mergeAllContracts()
        refresh()
    }

    fun createNew() = viewModelScope.launch {
        repository.createNewContract()
        refresh()
    }

    fun exportAll(uri: Uri) = viewModelScope.launch {
        repository.exportAll(uri)
    }

    fun importContract(uri: Uri) = viewModelScope.launch {
        repository.importContract(uri)
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        _contracts.value = repository.listContractInfo()
    }
}
