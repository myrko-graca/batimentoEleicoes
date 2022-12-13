drop table if exists UF;
drop table if exists MUN;
drop table if exists LOC;
drop table if exists VOT;
drop table if exists CAN;
CREATE TABLE UF (
	sigla 			char(2)		PRIMARY KEY,
	regiao			text 		NOT NULL
);
CREATE TABLE MUN (
	codigo			integer PRIMARY KEY,
	uf				char(2),
	nome			text,
	FOREIGN KEY (uf) REFERENCES UF(sigla)
);
CREATE TABLE LOC (
	codMun			text,
	zona			integer,
	secao			integer,
	numero			text,
	numeroUrna		integer,
	tipoUrna		text,
	qtdAptos		integer,
	dtEncerramento	datetime,
	dtRecebimento	datetime,
	PRIMARY KEY (codMun, zona, secao),
	FOREIGN KEY (codMun) REFERENCES MUN(codigo)
);
CREATE TABLE VOT (
	codMun			text,
	zona			integer,
	secao			integer,
	numCand			text,
	cargo			text,
	qtd				integer,
	FOREIGN KEY (codMun, zona, secao) REFERENCES LOC(codMun, zona, secao)
	FOREIGN KEY (numCand, cargo) REFERENCES CAN(numero, cargo)
);
CREATE TABLE CAN (
	numero			text,
	cargo			text,
	nome			text,
	partido			text,
	PRIMARY KEY (numero, cargo)
);
create view if not exists V_LOC (
	regiao,
	uf,
	codigo,
	nome,
	numero,
	zona,
	secao,
	numeroUrna,
	tipoUrna,
	qtdAptos,
	dtEncerramento,
	dtRecebimento
) as
select 
	u.regiao,
	m.uf,
	m.codigo,
	m.nome,
	l.numero,
	l.zona,
	l.secao,
	l.numeroUrna,
	l.tipoUrna,
	l.qtdAptos,
	l.dtEncerramento,
	l.dtRecebimento
from loc l
join mun m on m.codigo=l.codMun
join uf u on m.uf=u.sigla;
create view if not exists V_VOT (
	regiao,
	uf,
	codigo,
	nome,
	numero,
	zona,
	secao,
	numeroUrna,
	tipoUrna,
	qtdAptos,
	dtEncerramento,
	dtRecebimento,
	numCand,
	cargo,
	qtdVotos
) as
select 
	u.regiao,
	m.uf,
	m.codigo,
	m.nome,
	l.numero,
	l.zona,
	l.secao,
	l.numeroUrna,
	l.tipoUrna,
	l.qtdAptos,
	l.dtEncerramento,
	l.dtRecebimento,
	v.numCand,
	v.cargo,
	v.qtd
from vot v
join loc l on l.codMun=v.codMun and l.zona=v.zona and l.secao=v.secao
join mun m on m.codigo=l.codMun
join uf u on m.uf=u.sigla;


drop table if exists TStudent;
create table TStudent(espacoAmostral int, valor double);
insert into TStudent values (1, 636.61);
insert into TStudent values (2, 31.59);
insert into TStudent values (3, 12.92);
insert into TStudent values (4, 8.60);
insert into TStudent values (5, 6.86);
insert into TStudent values (6, 5.95);
insert into TStudent values (7, 5.40);
insert into TStudent values (8, 5.4);
insert into TStudent values (9, 4.78);
insert into TStudent values (10, 4.58);
insert into TStudent values (11, 4.43);
insert into TStudent values (12, 4.31);
insert into TStudent values (13, 4.22);
insert into TStudent values (14, 4.14);
insert into TStudent values (15, 4.7);
insert into TStudent values (16, 4.1);
insert into TStudent values (17, 3.96);
insert into TStudent values (18, 3.92);
insert into TStudent values (19, 3.88);
insert into TStudent values (20, 3.85);
insert into TStudent values (21, 3.81);
insert into TStudent values (22, 3.79);
insert into TStudent values (23, 3.76);
insert into TStudent values (24, 3.74);
insert into TStudent values (25, 3.72);
insert into TStudent values (26, 3.70);
insert into TStudent values (27, 3.69);
insert into TStudent values (28, 3.67);
insert into TStudent values (29, 3.65);
insert into TStudent values (30, 3.64);
insert into TStudent values (31, 3.64);
insert into TStudent values (32, 3.64);
insert into TStudent values (33, 3.64);
insert into TStudent values (34, 3.64);
insert into TStudent values (35, 3.64);
insert into TStudent values (36, 3.55);
insert into TStudent values (37, 3.55);
insert into TStudent values (38, 3.55);
insert into TStudent values (39, 3.55);
insert into TStudent values (40, 3.55);
insert into TStudent values (41, 3.55);
insert into TStudent values (42, 3.55);
insert into TStudent values (43, 3.55);
insert into TStudent values (44, 3.55);
insert into TStudent values (45, 3.55);
insert into TStudent values (46, 3.49);
insert into TStudent values (47, 3.49);
insert into TStudent values (48, 3.49);
insert into TStudent values (49, 3.49);
insert into TStudent values (50, 3.49);
