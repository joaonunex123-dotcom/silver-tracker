package com.stacking.tracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stacking.tracker.ContainerApp
import com.stacking.tracker.StackingApp
import com.stacking.tracker.ui.cotacao.CotacaoViewModel
import com.stacking.tracker.ui.dashboard.DashboardViewModel
import com.stacking.tracker.ui.detalhe.DetalheViewModel
import com.stacking.tracker.ui.editor.EditorViewModel
import com.stacking.tracker.ui.inventario.InventarioViewModel

private fun CreationExtras.container(): ContainerApp =
    (this[APPLICATION_KEY] as StackingApp).container

/** Fabrica unica para todos os ViewModels do app. */
val FabricaViewModel = viewModelFactory {
    initializer { DashboardViewModel(container()) }
    initializer { InventarioViewModel(container()) }
    initializer { CotacaoViewModel(container()) }
    initializer { EditorViewModel(container(), createSavedStateHandle()) }
    initializer { DetalheViewModel(container(), createSavedStateHandle()) }
}
