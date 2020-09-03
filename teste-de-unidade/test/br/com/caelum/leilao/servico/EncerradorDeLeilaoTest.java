package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.repositorio.EnviadorDeEmail;
import br.com.caelum.leilao.repositorio.RepositorioDeLeiloes;

public class EncerradorDeLeilaoTest {

	@Test
	public void deveEncerrarLeiloesQueComecemUmaSemanaAntes() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("Tv").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
		List<Leilao> leiloesAntigo = Arrays.asList(leilao1, leilao2);

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);

		when(daoFalso.correntes()).thenReturn(leiloesAntigo);

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		assertEquals(2, encerrar.getTotalEncerrados());
		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());

	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramOntem() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(2020, 05, 04);

		Leilao leilao1 = new CriadorDeLeilao().para("Tv").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
		List<Leilao> leiloesAntigo = Arrays.asList(leilao1, leilao2);

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);

		when(daoFalso.correntes()).thenReturn(leiloesAntigo);

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		assertEquals(0, encerrar.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());

	}

	@Test
	public void naoExisteLeialao() {
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);

		when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		assertEquals(0, encerrar.getTotalEncerrados());
	}

	@Test
	public void daveAtualizarLeiloesEncerrados() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao l1 = new CriadorDeLeilao().para("Tv").naData(antiga).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(l1));

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		verify(daoFalso, times(1)).atualiza(l1);
	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {

		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		assertEquals(0, encerrar.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());

		verify(daoFalso, never()).atualiza(leilao1);
		verify(daoFalso, never()).atualiza(leilao2);
	}

	@Test
	public void vareificaEnvioEmail() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao l1 = new CriadorDeLeilao().para("Tv").naData(antiga).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(l1));

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		InOrder inOrder = inOrder(daoFalso, carteiroFalso);

		inOrder.verify(daoFalso, times(1)).atualiza(l1);
		inOrder.verify(carteiroFalso, times(1)).envia(l1);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoFalha() {

		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);

		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeErrorException(null)).when(daoFalso).atualiza(leilao1);

		EncerradorDeLeilao encerrar = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrar.encerra();

		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);

		verify(carteiroFalso, times(0)).envia(leilao1);
	}

	@Test
	public void deveDesistirSeDaoFalhaPraSempre() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();

		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);

		encerrador.encerra();

		verify(carteiroFalso, never()).envia(any(Leilao.class));
	}

}
