package br.com.infnet.shared.service;

import java.util.List;

public interface CrudService<T, ID> {
    T buscarPorId(ID id);
    List<T> listarTodos();
    void remover(ID id);
}
