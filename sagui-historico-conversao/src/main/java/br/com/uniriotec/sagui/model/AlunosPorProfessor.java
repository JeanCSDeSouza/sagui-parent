package br.com.uniriotec.sagui.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * The persistent class for the alunos_por_professor database table.
 * 
 */
@Entity
@Immutable
@Table(name="alunos_por_professor")
@NamedQuery(name="AlunosPorProfessor.findAll", query="SELECT a FROM AlunosPorProfessor a")
@NoArgsConstructor
public class AlunosPorProfessor implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(nullable=false)
	@Getter @Setter private Long aluno_Id;

	@Column(length=300)
	@Getter @Setter private String email;

	@Column(nullable=false, length=45)
	@Getter @Setter private String matricula;

	@Column(length=300)
	@Getter @Setter private String nome;

	@Column(name="nome_aluno", length=300)
	@Getter @Setter private String nomeAluno;

	@Column(name="PERIODO_TUTOR", nullable=false, length=5)
	@Getter @Setter private String periodoTutor;

	@Column(name="usuario_id", nullable=false)
	@Getter @Setter private Long usuarioId;
}